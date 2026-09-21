package br.com.fpo.sgc.domain.notification;

import br.com.fpo.sgc.domain.serviceorder.ServiceOrder;
import br.com.fpo.sgc.domain.user.User;

public interface NotificationService {

    /**
     * Notifica o destinatário sobre uma ordem de serviço: tenta o WhatsApp (com retentativas) e, se
     * falhar e o fallback estiver habilitado, o e-mail. Cada tentativa é registrada em tb_notifications.
     *
     * @return true se alguma tentativa de entrega teve sucesso
     */
    boolean notifyUser(ServiceOrder serviceOrder, User recipient, String subject, String message);
}
