package br.com.fpo.sgc.domain.notification;

import br.com.fpo.sgc.domain.serviceorder.ServiceOrder;
import br.com.fpo.sgc.domain.serviceorder.ServiceOrderStatus;
import br.com.fpo.sgc.domain.user.User;

public interface NotificationService {

    /**
     * Notifica o destinatário sobre uma ordem de serviço: tenta o WhatsApp (com retentativas) e, se
     * falhar e o fallback estiver habilitado, o e-mail. Cada tentativa é registrada em tb_notifications.
     *
     * @param status status da OS no momento do evento (usado para a apresentação da mensagem)
     * @return true se alguma tentativa de entrega teve sucesso
     */
    boolean notifyUser(ServiceOrder serviceOrder, User recipient, String subject, String message,
                       ServiceOrderStatus status);

    /**
     * Trata a falha de entrega que a Meta informa depois do envio, pelo webhook (status "failed"): a Cloud
     * API responde 200 ao aceitar a mensagem, mas a entrega pode falhar em seguida (ex.: fora da janela de
     * 24 h). Marca a tentativa do WhatsApp como falha e, com o fallback habilitado, reenvia por e-mail (RN08).
     * Ids desconhecidos (respostas automáticas, que não são notificações de OS) são ignorados.
     *
     * @param providerMessageId id da mensagem na Meta (wamid)
     * @param errorDetail       descrição do erro informada no webhook (pode ser null)
     */
    void handleAsyncDeliveryFailure(String providerMessageId, String errorDetail);
}
