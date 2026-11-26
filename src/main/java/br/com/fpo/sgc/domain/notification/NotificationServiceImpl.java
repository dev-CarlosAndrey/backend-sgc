package br.com.fpo.sgc.domain.notification;

import br.com.fpo.sgc.domain.serviceorder.ServiceOrder;
import br.com.fpo.sgc.domain.serviceorder.ServiceOrderStatus;
import br.com.fpo.sgc.domain.user.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Não é @Transactional de propósito: cada tentativa é gravada e confirmada na hora (o registro de falha
 * precisa sobreviver a qualquer rollback do chamador) e não se deve manter transação aberta durante
 * chamadas HTTP e esperas de backoff. Quem dispara deve chamar após o commit da alteração da OS.
 */
@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    private static final int MAX_ERROR_LENGTH = 500;
    private static final int MAX_SUBJECT_LENGTH = 255;
    private static final int MAX_MESSAGE_LENGTH = 2000;

    private final Map<NotificationChannel, NotificationSender> senders = new EnumMap<>(NotificationChannel.class);
    private final NotificationRepository notificationRepository;
    private final boolean fallbackEnabled;
    private final int maxAttempts;
    private final long retryBackoffMs;

    public NotificationServiceImpl(
            List<NotificationSender> senderBeans,
            NotificationRepository notificationRepository,
            @Value("${notification.fallback-enabled:true}") boolean fallbackEnabled,
            @Value("${notification.max-attempts:3}") int maxAttempts,
            @Value("${notification.retry-backoff-ms:1000}") long retryBackoffMs) {
        senderBeans.forEach(sender -> senders.put(sender.channel(), sender));
        this.notificationRepository = notificationRepository;
        this.fallbackEnabled = fallbackEnabled;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.retryBackoffMs = Math.max(0, retryBackoffMs);
    }

    @Override
    public boolean notifyUser(ServiceOrder serviceOrder, User recipient, String subject, String message,
                              ServiceOrderStatus status) {
        if (deliver(NotificationChannel.WHATSAPP, serviceOrder, recipient, subject, message, status)) {
            return true;
        }
        if (fallbackEnabled && deliver(NotificationChannel.EMAIL, serviceOrder, recipient, subject, message, status)) {
            return true;
        }
        log.warn("Notificação da OS {} não entregue ao usuário {} por nenhum canal disponível",
                serviceOrder.getId(), recipient.getId());
        return false;
    }

    // Tenta um canal até maxAttempts vezes com backoff linear; false se o canal não existe, não se aplica ou falhou.
    private boolean deliver(NotificationChannel channel, ServiceOrder serviceOrder, User recipient,
                            String subject, String message, ServiceOrderStatus status) {
        NotificationSender sender = senders.get(channel);
        if (sender == null || !sender.canSend(recipient)) {
            return false;
        }

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                String providerMessageId = sender.send(recipient, subject, message, status);
                Notification sent = buildRecord(serviceOrder, recipient, channel, NotificationStatus.SENT, attempt, null);
                if (channel == NotificationChannel.WHATSAPP) {
                    // Guardados para o fallback assíncrono (handleAsyncDeliveryFailure).
                    sent.setProviderMessageId(providerMessageId);
                    sent.setSubject(truncate(subject, MAX_SUBJECT_LENGTH));
                    sent.setMessageBody(truncate(message, MAX_MESSAGE_LENGTH));
                }
                notificationRepository.save(sent);
                return true;
            } catch (Exception e) {
                String detail = errorDetail(e);
                log.warn("Falha ao notificar via {} (tentativa {}/{}): {}", channel, attempt, maxAttempts, detail);
                record(serviceOrder, recipient, channel, NotificationStatus.FAILED, attempt, detail);
                if (!isTransient(e)) {
                    // Token inválido, número fora da lista, requisição malformada...: repetir não resolve.
                    log.warn("Erro não transitório via {}: sem novas tentativas neste canal", channel);
                    return false;
                }
                if (attempt < maxAttempts && !waitBeforeRetry(attempt)) {
                    return false;
                }
            }
        }
        return false;
    }

    @Override
    @Async
    public void handleAsyncDeliveryFailure(String providerMessageId, String errorDetail) {
        if (providerMessageId == null || providerMessageId.isBlank()) {
            return;
        }
        List<Notification> matches = notificationRepository
                .findByProviderMessageIdFetchingOrderAndRecipient(providerMessageId, NotificationChannel.WHATSAPP);
        if (matches.isEmpty()) {
            log.debug("Falha assíncrona do WhatsApp para mensagem sem notificação de OS associada ({})", providerMessageId);
            return;
        }
        Notification whatsappAttempt = matches.get(0);
        if (whatsappAttempt.getDeliveryStatus() == NotificationStatus.FAILED) {
            // A Meta pode reenviar o mesmo evento: o fallback já foi tratado.
            return;
        }

        String detail = "Falha informada pela Meta após o envio" + (errorDetail != null ? ": " + errorDetail : "");
        whatsappAttempt.setDeliveryStatus(NotificationStatus.FAILED);
        whatsappAttempt.setErrorMessage(truncate(detail, MAX_ERROR_LENGTH));
        notificationRepository.save(whatsappAttempt);

        ServiceOrder serviceOrder = whatsappAttempt.getServiceOrder();
        User recipient = whatsappAttempt.getRecipient();
        log.warn("Entrega da notificação da OS {} ao usuário {} falhou no WhatsApp ({}): {}",
                serviceOrder.getId(), recipient.getId(), providerMessageId, detail);

        if (!fallbackEnabled || whatsappAttempt.getMessageBody() == null) {
            return;
        }
        if (!deliver(NotificationChannel.EMAIL, serviceOrder, recipient, whatsappAttempt.getSubject(),
                whatsappAttempt.getMessageBody(), serviceOrder.getStatus())) {
            log.warn("Notificação da OS {} não entregue ao usuário {} por nenhum canal após a falha assíncrona",
                    serviceOrder.getId(), recipient.getId());
        }
    }

    // 4xx (exceto 408 e 429) indica erro que não se resolve repetindo; 5xx, 408, 429 e falhas de rede, sim.
    static boolean isTransient(Exception e) {
        if (e instanceof RestClientResponseException responseException) {
            int code = responseException.getStatusCode().value();
            return code >= 500 || code == 408 || code == 429;
        }
        return true;
    }

    // Erros HTTP (token expirado, número restrito, limite de mensagens...) trazem o corpo de erro da API
    // (Graph API ou SendGrid) na resposta; usar isso em vez da mensagem genérica da exceção.
    private String errorDetail(Exception e) {
        if (e instanceof RestClientResponseException responseException) {
            String body = responseException.getResponseBodyAsString();
            return body != null && !body.isBlank() ? body : e.getMessage();
        }
        return e.getMessage();
    }

    private boolean waitBeforeRetry(int attempt) {
        try {
            Thread.sleep(retryBackoffMs * attempt);
            return true;
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void record(ServiceOrder serviceOrder, User recipient, NotificationChannel channel,
                        NotificationStatus status, int attempt, String error) {
        notificationRepository.save(buildRecord(serviceOrder, recipient, channel, status, attempt, error));
    }

    private Notification buildRecord(ServiceOrder serviceOrder, User recipient, NotificationChannel channel,
                                     NotificationStatus status, int attempt, String error) {
        return Notification.builder()
                .serviceOrder(serviceOrder)
                .recipient(recipient)
                .channel(channel)
                .deliveryStatus(status)
                .attempt(attempt)
                .errorMessage(truncate(error, MAX_ERROR_LENGTH))
                .build();
    }

    private static String truncate(String value, int max) {
        return value != null && value.length() > max ? value.substring(0, max) : value;
    }
}
