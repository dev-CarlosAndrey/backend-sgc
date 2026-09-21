package br.com.fpo.sgc.domain.notification;

import br.com.fpo.sgc.domain.serviceorder.*;
import br.com.fpo.sgc.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ServiceOrderNotificationListenerTest {

    private ServiceOrderRepository repository;
    private NotificationService notificationService;
    private ServiceOrderNotificationListener listener;
    private User requester;
    private User technician;

    @BeforeEach
    void setUp() {
        repository = mock(ServiceOrderRepository.class);
        notificationService = mock(NotificationService.class);
        listener = new ServiceOrderNotificationListener(repository, notificationService);
        requester = User.builder().id(1L).name("Prof").build();
        technician = User.builder().id(2L).name("Téc").build();
    }

    private ServiceOrder order(User tech) {
        ServiceOrder so = ServiceOrder.builder().id(10L).title("Projetor").requester(requester).technician(tech).build();
        when(repository.findWithParticipantsById(10L)).thenReturn(Optional.of(so));
        return so;
    }

    @Test
    void notificaSolicitanteETecnico() {
        order(technician);

        listener.onServiceOrderEvent(new ServiceOrderEvent(10L, ServiceOrderEventType.TECHNICIAN_ASSIGNED, ServiceOrderStatus.IN_PROGRESS));

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(notificationService).notifyUser(any(), eq(requester), eq("OS #10 - Projetor"), message.capture(), eq(ServiceOrderStatus.IN_PROGRESS));
        verify(notificationService).notifyUser(any(), eq(technician), any(), any(), any());
        assertTrue(message.getValue().contains("Téc"));
    }

    @Test
    void semTecnico_notificaSoSolicitante() {
        order(null);

        listener.onServiceOrderEvent(new ServiceOrderEvent(10L, ServiceOrderEventType.CREATED, ServiceOrderStatus.OPEN));

        verify(notificationService, times(1)).notifyUser(any(), any(), any(), any(), any());
        verify(notificationService).notifyUser(any(), eq(requester), any(), any(), any());
    }

    @Test
    void solicitanteIgualAoTecnico_notificaUmaVez() {
        order(requester);

        listener.onServiceOrderEvent(new ServiceOrderEvent(10L, ServiceOrderEventType.STATUS_CHANGED, ServiceOrderStatus.CANCELLED));

        verify(notificationService, times(1)).notifyUser(any(), any(), any(), any(), any());
    }

    @Test
    void usuarioInativo_naoRecebe() {
        technician.setActive(false);
        order(technician);

        listener.onServiceOrderEvent(new ServiceOrderEvent(10L, ServiceOrderEventType.RESOLVED, ServiceOrderStatus.RESOLVED));

        verify(notificationService, never()).notifyUser(any(), eq(technician), any(), any(), any());
        verify(notificationService).notifyUser(any(), eq(requester), any(), any(), any());
    }

    @Test
    void falhaNaNotificacao_naoPropaga() {
        order(technician);
        when(notificationService.notifyUser(any(), any(), any(), any(), any())).thenThrow(new RuntimeException("boom"));

        assertDoesNotThrow(() -> listener.onServiceOrderEvent(
                new ServiceOrderEvent(10L, ServiceOrderEventType.CREATED, ServiceOrderStatus.OPEN)));
    }

    @Test
    void osInexistente_naoNotifica() {
        when(repository.findWithParticipantsById(99L)).thenReturn(Optional.empty());

        listener.onServiceOrderEvent(new ServiceOrderEvent(99L, ServiceOrderEventType.CREATED, ServiceOrderStatus.OPEN));

        verifyNoInteractions(notificationService);
    }
}
