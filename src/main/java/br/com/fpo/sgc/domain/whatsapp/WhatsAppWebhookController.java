package br.com.fpo.sgc.domain.whatsapp;

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

/**
 * Webhook do WhatsApp Cloud API (Meta): verificação de assinatura (GET, desafio de configuração) e
 * recebimento de eventos de mensagem (POST). RF14. Liberado no SecurityConfig (a Meta não envia JWT).
 *
 * O processamento do conteúdo das mensagens (abertura/consulta de chamado via chatbot) é o RF15/RF16,
 * ainda não implementado: por ora este endpoint apenas valida a origem da chamada e confirma o recebimento,
 * respondendo rápido como exige a integração (o processamento assíncrono virá junto do RF15).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/whatsapp/webhook")
public class WhatsAppWebhookController {

    private final WhatsAppSignatureVerifier signatureVerifier;
    private final String verifyToken;

    public WhatsAppWebhookController(
            WhatsAppSignatureVerifier signatureVerifier,
            @Value("${whatsapp.cloud-api.webhook-verify-token:}") String verifyToken) {
        this.signatureVerifier = signatureVerifier;
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
        log.info("Evento recebido no webhook do WhatsApp ({} bytes)", rawBody.length());
        return ResponseEntity.ok().build();
    }
}
