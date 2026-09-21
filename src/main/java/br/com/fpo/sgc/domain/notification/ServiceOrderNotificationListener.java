package br.com.fpo.sgc.domain.notification;

import br.com.fpo.sgc.domain.serviceorder.ServiceOrder;
import br.com.fpo.sgc.domain.serviceorder.ServiceOrderEvent;
import br.com.fpo.sgc.domain.serviceorder.ServiceOrderRepository;
import br.com.fpo.sgc.domain.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Notifica solicitante e técnico nos eventos da OS. Roda só depois do commit e em outra thread: uma
 * falha (ou lentidão) de notificação nunca desfaz nem atrasa a operação que a originou.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceOrderNotificationListener {

    private final ServiceOrderRepository serviceOrderRepository;
    private final NotificationService notificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onServiceOrderEvent(ServiceOrderEvent event) {
        try {
            ServiceOrder so = serviceOrderRepository.findWithParticipantsById(event.serviceOrderId()).orElse(null);
            if (so == null) {
                log.warn("OS {} não encontrada ao disparar notificações", event.serviceOrderId());
                return;
            }

            String subject = "OS #" + so.getId() + " - " + so.getTitle();
            String message = buildMessage(so, event);
            for (User recipient : recipients(so)) {
                notificationService.notifyUser(so, recipient, subject, message);
            }
        } catch (Exception e) {
            log.error("Erro ao disparar notificações da OS {}", event.serviceOrderId(), e);
        }
    }

    // Solicitante e técnico (quando houver), sem duplicar e ignorando usuários inativos.
    private Set<User> recipients(ServiceOrder so) {
        Set<User> recipients = new LinkedHashSet<>();
        for (User user : new User[]{so.getRequester(), so.getTechnician()}) {
            if (user != null && Boolean.TRUE.equals(user.getActive())) {
                recipients.add(user);
            }
        }
        return recipients;
    }

    private String buildMessage(ServiceOrder so, ServiceOrderEvent event) {
        String ref = "OS #" + so.getId() + " (" + so.getTitle() + ")";
        return switch (event.type()) {
            case CREATED -> "Sua solicitação foi registrada: " + ref + ". Você será avisado a cada atualização.";
            case TECHNICIAN_ASSIGNED -> "A " + ref + " foi atribuída ao técnico "
                    + so.getTechnician().getName() + " e está em andamento.";
            case STATUS_CHANGED -> "A " + ref + " mudou de status: " + statusLabel(event) + ".";
            case RESOLVED -> "A " + ref + " foi concluída."
                    + (so.getSolutionDetails() != null ? " Laudo: " + so.getSolutionDetails().trim() : "");
        };
    }

    private String statusLabel(ServiceOrderEvent event) {
        return switch (event.status()) {
            case OPEN -> "aberta";
            case IN_PROGRESS -> "em andamento";
            case RESOLVED -> "concluída";
            case CANCELLED -> "cancelada";
        };
    }
}
