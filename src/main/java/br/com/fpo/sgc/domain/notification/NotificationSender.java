package br.com.fpo.sgc.domain.notification;

import br.com.fpo.sgc.domain.serviceorder.ServiceOrderStatus;
import br.com.fpo.sgc.domain.user.User;

/**
 * Canal de entrega de notificações (WhatsApp, e-mail...). Cada canal é um bean; o
 * {@link NotificationService} escolhe entre eles e aplica retentativa e fallback.
 */
public interface NotificationSender {

    NotificationChannel channel();

    /** Indica se o destinatário possui o dado de contato exigido por este canal (telefone, e-mail...). */
    boolean canSend(User recipient);

    /**
     * Envia a mensagem; qualquer falha de entrega deve ser propagada como exceção. O status da OS permite
     * ao canal adaptar a apresentação (cor, rótulo, progresso).
     */
    void send(User recipient, String subject, String message, ServiceOrderStatus status);
}
