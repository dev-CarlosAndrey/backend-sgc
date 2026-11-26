package br.com.fpo.sgc.domain.whatsapp;

import br.com.fpo.sgc.domain.notification.NotificationService;
import br.com.fpo.sgc.domain.whatsapp.dto.WhatsAppWebhookPayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Webhook do WhatsApp Cloud API (Meta): verificação de assinatura (GET, desafio de configuração) e
 * recebimento de eventos de mensagem (POST). RF14. Liberado no SecurityConfig (a Meta não envia JWT).
 *
 * Toda mensagem de texto recebida é repassada a {@link WhatsAppAutoReplyService} (RF15/RF16): se estiver no
 * formato de abertura de OS, o chamado é criado e a pessoa recebe a confirmação (ou o erro, se o setor/bem
 * não existir); se for um comando "Status", recebe o status do(s) chamado(s) associado(s) ao telefone;
 * caso contrário, recebe a mensagem de boas-vindas com os formatos esperados. Este endpoint só valida a
 * origem da chamada, faz o parsing do payload, loga mensagens recebidas e atualizações de status de
 * entrega, e responde rápido como exige a integração — o processamento da mensagem é assíncrono para não
 * atrasar essa resposta.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/whatsapp/webhook")
public class WhatsAppWebhookController {

    // spring-boot-starter-webmvc, ao contrário do antigo starter-web, não expõe um bean ObjectMapper
    // pronto para injeção; instanciamos um próprio, igual aos testes deste controller.
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final WhatsAppSignatureVerifier signatureVerifier;
    private final WhatsAppAutoReplyService autoReplyService;
    private final WhatsAppInboundMessageRegistry inboundMessageRegistry;
    private final NotificationService notificationService;
    private final String verifyToken;

    public WhatsAppWebhookController(
            WhatsAppSignatureVerifier signatureVerifier,
            WhatsAppAutoReplyService autoReplyService,
            WhatsAppInboundMessageRegistry inboundMessageRegistry,
            NotificationService notificationService,
            @Value("${whatsapp.cloud-api.webhook-verify-token:}") String verifyToken) {
        this.signatureVerifier = signatureVerifier;
        this.autoReplyService = autoReplyService;
        this.inboundMessageRegistry = inboundMessageRegistry;
        this.notificationService = notificationService;
        this.verifyToken = verifyToken;
    }

    @GetMapping
    public ResponseEntity<String> verify(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String token,
            @RequestParam(name = "hub.challenge", required = false) String challenge) {
        boolean tokenConfigured = verifyToken != null && !verifyToken.isBlank() && !verifyToken.equals("defina_no_ambiente");
        if (tokenConfigured && "subscribe".equals(mode) && verifyToken.equals(token) && challenge != null) {
            log.info("Webhook do WhatsApp verificado com sucesso pela Meta");
            return ResponseEntity.ok(challenge);
        }
        log.warn("Falha na verificação do webhook do WhatsApp (mode={})", mode);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @PostMapping
    public ResponseEntity<Void> receive(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody String rawBody) {
        if (!signatureVerifier.isValid(rawBody, signature)) {
            log.warn("Assinatura inválida em evento recebido no webhook do WhatsApp");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        logPayload(rawBody);
        return ResponseEntity.ok().build();
    }

    // A Meta já validou a assinatura acima; um payload malformado aqui não é motivo para reprocessar
    // (não é isso que corrigiria), então só logamos e ainda assim confirmamos o recebimento.
    private void logPayload(String rawBody) {
        WhatsAppWebhookPayload payload;
        try {
            payload = objectMapper.readValue(rawBody, WhatsAppWebhookPayload.class);
        } catch (JsonProcessingException e) {
            log.warn("Payload do webhook do WhatsApp não pôde ser interpretado: {}", e.getMessage());
            return;
        }

        for (WhatsAppWebhookPayload.Entry entry : orEmpty(payload.entry())) {
            for (WhatsAppWebhookPayload.Change change : orEmpty(entry.changes())) {
                WhatsAppWebhookPayload.Value value = change.value();
                if (value == null) {
                    continue;
                }
                for (WhatsAppWebhookPayload.IncomingMessage message : orEmpty(value.messages())) {
                    if (!inboundMessageRegistry.registerIfNew(message.id(), message.from())) {
                        log.info("Mensagem {} do WhatsApp já processada (reenvio da Meta): ignorada", message.id());
                        continue;
                    }
                    String body = message.text() != null ? message.text().body() : null;
                    log.info("Mensagem recebida no WhatsApp de {} ({}): {}", message.from(), message.type(), body);
                    autoReplyService.handleIncomingMessage(message.from(), displayNameOf(message.from(), value.contacts()), body);
                }
                for (WhatsAppWebhookPayload.MessageStatus status : orEmpty(value.statuses())) {
                    log.info("Atualização de status no WhatsApp: mensagem {} -> {} (destinatário {})",
                            status.id(), status.status(), status.recipientId());
                    if ("failed".equals(status.status())) {
                        // A entrega falhou depois do 200 do envio: marca a falha e aciona o e-mail (RN08).
                        notificationService.handleAsyncDeliveryFailure(status.id(), describeErrors(status.errors()));
                    }
                }
            }
        }
    }

    // A Meta manda o nome de exibição do contato em "contacts", casado pelo wa_id = telefone de quem mandou.
    private String displayNameOf(String fromPhone, List<WhatsAppWebhookPayload.Contact> contacts) {
        return orEmpty(contacts).stream()
                .filter(contact -> fromPhone.equals(contact.waId()))
                .map(contact -> contact.profile() != null ? contact.profile().name() : null)
                .filter(name -> name != null && !name.isBlank())
                .findFirst()
                .orElse(null);
    }

    private String describeErrors(List<WhatsAppWebhookPayload.StatusError> errors) {
        return orEmpty(errors).stream()
                .map(error -> error.code() + " " + (error.title() != null ? error.title() : "")
                        + (error.message() != null ? " - " + error.message() : ""))
                .map(String::trim)
                .reduce((a, b) -> a + "; " + b)
                .orElse(null);
    }

    private <T> List<T> orEmpty(List<T> list) {
        return list != null ? list : List.of();
    }
}
