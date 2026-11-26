package br.com.fpo.sgc.domain.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import br.com.fpo.sgc.domain.serviceorder.ServiceOrderStatus;
import br.com.fpo.sgc.domain.user.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Canal de WhatsApp via WhatsApp Cloud API (Meta), POST /{phone-number-id}/messages. Sem
 * WHATSAPP_ACCESS_TOKEN/WHATSAPP_PHONE_NUMBER_ID configurados o canal fica indisponível (canSend = false)
 * e o NotificationService cai direto para o fallback de e-mail.
 *
 * Envia como mensagem de texto livre (não usa template), o que só é entregue dentro da janela de
 * atendimento de 24h desde a última mensagem do destinatário — suficiente para o caso de uso de
 * notificação de OS em resposta a um chamado aberto pelo próprio WhatsApp (RF15/RF16, ainda não implementado).
 */
@Slf4j
@Component
public class WhatsAppNotificationSender implements NotificationSender {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final RestClient restClient;
    private final String accessToken;
    private final String phoneNumberId;

    @Autowired
    public WhatsAppNotificationSender(
            @Value("${whatsapp.cloud-api.base-url:https://graph.facebook.com/v22.0}") String baseUrl,
            @Value("${whatsapp.cloud-api.access-token:}") String accessToken,
            @Value("${whatsapp.cloud-api.phone-number-id:}") String phoneNumberId) {
        this(RestClient.builder(), baseUrl, accessToken, phoneNumberId);
    }

    // Permite injetar um RestClient.Builder (ex.: MockRestServiceServer nos testes).
    WhatsAppNotificationSender(RestClient.Builder restClientBuilder, String baseUrl, String accessToken, String phoneNumberId) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.accessToken = accessToken;
        this.phoneNumberId = phoneNumberId;
        if (!isConfigured()) {
            log.warn("WhatsApp Cloud API não configurada (WHATSAPP_ACCESS_TOKEN/WHATSAPP_PHONE_NUMBER_ID): canal desabilitado");
        }
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.WHATSAPP;
    }

    @Override
    public boolean canSend(User recipient) {
        return isConfigured() && recipient.getPhone() != null && !recipient.getPhone().isBlank();
    }

    @Override
    public String send(User recipient, String subject, String message, ServiceOrderStatus status) {
        String to = recipient.getPhone().replaceAll("\\D", "");
        String text = subject == null || subject.isBlank() ? message : subject + "\n\n" + message;
        return sendText(to, text);
    }

    // Reaproveitado pelo WhatsAppAutoReplyService (RF14): o destinatário de uma resposta automática no
    // webhook não é necessariamente um User cadastrado, então o envio cru por telefone fica público aqui.
    // Devolve o id da mensagem (wamid) informado pela Meta, ou null se a resposta não o trouxer. O 200 só
    // significa "aceita": a entrega pode falhar depois e chegar como status "failed" no webhook (RN08).
    public String sendText(String toPhoneDigitsOnly, String text) {
        Map<String, Object> body = Map.of(
                "messaging_product", "whatsapp",
                "to", toPhoneDigitsOnly,
                "type", "text",
                "text", Map.of("body", text));

        // 200 em caso de sucesso; 4xx/5xx (token expirado, número restrito, limite excedido...) viram
        // RestClientResponseException, tratada pelo NotificationService (retentativa e fallback por e-mail)
        // ou pelo WhatsAppAutoReplyService (que só loga, pois não há fallback para uma resposta automática).
        String response = restClient.post()
                .uri("/{phoneNumberId}/messages", phoneNumberId)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);
        return extractMessageId(response);
    }

    // Resposta da Cloud API: {"messaging_product":"whatsapp","contacts":[...],"messages":[{"id":"wamid..."}]}
    String extractMessageId(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }
        try {
            JsonNode id = objectMapper.readTree(responseBody).path("messages").path(0).path("id");
            return id.isTextual() ? id.asText() : null;
        } catch (JsonProcessingException e) {
            log.warn("Resposta da Cloud API sem JSON válido; id da mensagem não registrado: {}", e.getMessage());
            return null;
        }
    }

    // Ignora vazio e o placeholder de exemplo do application.yml.
    public boolean isConfigured() {
        return accessToken != null && !accessToken.isBlank() && !accessToken.equals("defina_no_ambiente")
                && phoneNumberId != null && !phoneNumberId.isBlank() && !phoneNumberId.equals("defina_no_ambiente");
    }
}
