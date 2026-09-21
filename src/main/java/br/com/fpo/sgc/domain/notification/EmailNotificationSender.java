package br.com.fpo.sgc.domain.notification;

import br.com.fpo.sgc.domain.user.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Canal de e-mail via API v3 do SendGrid (POST /v3/mail/send). Sem SENDGRID_API_KEY / SENDGRID_FROM_EMAIL
 * configurados o canal fica indisponível (canSend = false) e o NotificationService o ignora.
 */
@Slf4j
@Component
public class EmailNotificationSender implements NotificationSender {

    private final RestClient restClient;
    private final String apiKey;
    private final String fromEmail;
    private final String fromName;

    @Autowired
    public EmailNotificationSender(
            @Value("${sendgrid.base-url:https://api.sendgrid.com}") String baseUrl,
            @Value("${sendgrid.api-key:}") String apiKey,
            @Value("${sendgrid.from-email:}") String fromEmail,
            @Value("${sendgrid.from-name:SGC}") String fromName) {
        this(RestClient.builder(), baseUrl, apiKey, fromEmail, fromName);
    }

    // Permite injetar um RestClient.Builder (ex.: MockRestServiceServer nos testes).
    EmailNotificationSender(
            RestClient.Builder restClientBuilder,
            String baseUrl,
            String apiKey,
            String fromEmail,
            String fromName) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.fromEmail = fromEmail;
        this.fromName = fromName;
        if (!isConfigured()) {
            log.warn("SendGrid não configurado (SENDGRID_API_KEY/SENDGRID_FROM_EMAIL): fallback por e-mail desabilitado");
        }
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public boolean canSend(User recipient) {
        return isConfigured() && recipient.getEmail() != null && !recipient.getEmail().isBlank();
    }

    @Override
    public void send(User recipient, String subject, String message) {
        Map<String, Object> body = Map.of(
                "personalizations", List.of(Map.of("to", List.of(
                        Map.of("email", recipient.getEmail(), "name", recipient.getName())))),
                "from", Map.of("email", fromEmail, "name", fromName),
                "subject", subject,
                "content", List.of(Map.of("type", "text/plain", "value", message)));

        // 202 Accepted em caso de sucesso; 4xx/5xx viram RestClientResponseException, tratada pelo NotificationService.
        restClient.post()
                .uri("/v3/mail/send")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    // Ignora vazio e o placeholder de exemplo.
    private boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && apiKey.startsWith("SG.")
                && fromEmail != null && !fromEmail.isBlank();
    }
}
