package br.com.fpo.sgc.domain.notification;

import br.com.fpo.sgc.domain.serviceorder.ServiceOrder;
import br.com.fpo.sgc.domain.user.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
    public boolean notifyUser(ServiceOrder serviceOrder, User recipient, String subject, String message) {
        if (deliver(NotificationChannel.WHATSAPP, serviceOrder, recipient, subject, message)) {
            return true;
        }
        if (fallbackEnabled && deliver(NotificationChannel.EMAIL, serviceOrder, recipient, subject, message)) {
            return true;
        }
        log.warn("Notificação da OS {} não entregue ao usuário {} por nenhum canal disponível",
                serviceOrder.getId(), recipient.getId());
        return false;
    }

    // Tenta um canal até maxAttempts vezes com backoff linear; false se o canal não existe, não se aplica ou falhou.
    private boolean deliver(NotificationChannel channel, ServiceOrder serviceOrder, User recipient,
                            String subject, String message) {
        NotificationSender sender = senders.get(channel);
        if (sender == null || !sender.canSend(recipient)) {
            return false;
        }

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                sender.send(recipient, subject, message);
                record(serviceOrder, recipient, channel, NotificationStatus.SENT, attempt, null);
                return true;
            } catch (Exception e) {
                log.warn("Falha ao notificar via {} (tentativa {}/{}): {}", channel, attempt, maxAttempts, e.getMessage());
                record(serviceOrder, recipient, channel, NotificationStatus.FAILED, attempt, e.getMessage());
                if (attempt < maxAttempts && !waitBeforeRetry(attempt)) {
                    return false;
                }
            }
        }
        return false;
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
        String truncated = error != null && error.length() > MAX_ERROR_LENGTH
                ? error.substring(0, MAX_ERROR_LENGTH) : error;
        notificationRepository.save(Notification.builder()
                .serviceOrder(serviceOrder)
                .recipient(recipient)
                .channel(channel)
                .deliveryStatus(status)
                .attempt(attempt)
                .errorMessage(truncated)
                .build());
    }
}
