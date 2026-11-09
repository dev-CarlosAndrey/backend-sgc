package br.com.fpo.sgc.domain.notification;

import br.com.fpo.sgc.domain.serviceorder.*;
import br.com.fpo.sgc.domain.user.User;
import br.com.fpo.sgc.domain.user.UserRepository;
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
    private UserRepository userRepository;
    private ServiceOrderNotificationListener listener;
    private User requester;
    private User technician;

    @BeforeEach
    void setUp() {
        repository = mock(ServiceOrderRepository.class);
        notificationService = mock(NotificationService.class);
        userRepository = mock(UserRepository.class);
        listener = new ServiceOrderNotificationListener(repository, userRepository, notificationService);
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

    @Test
    void quemResolveu_tambemRecebe_mesmoSendoDiferenteDoTecnicoAtribuido() {
        order(technician);
        User resolver = User.builder().id(3L).name("Admin").build();
        when(userRepository.findById(3L)).thenReturn(Optional.of(resolver));

        listener.onServiceOrderEvent(new ServiceOrderEvent(10L, ServiceOrderEventType.RESOLVED, ServiceOrderStatus.RESOLVED, 3L));

        verify(notificationService, times(3)).notifyUser(any(), any(), any(), any(), any());
        verify(notificationService).notifyUser(any(), eq(resolver), any(), any(), eq(ServiceOrderStatus.RESOLVED));
    }

    @Test
    void osSemTecnico_quemResolveuRecebeJuntoComOSolicitante() {
        order(null);
        User resolver = User.builder().id(3L).name("Téc").build();
        when(userRepository.findById(3L)).thenReturn(Optional.of(resolver));

        listener.onServiceOrderEvent(new ServiceOrderEvent(10L, ServiceOrderEventType.RESOLVED, ServiceOrderStatus.RESOLVED, 3L));

        verify(notificationService, times(2)).notifyUser(any(), any(), any(), any(), any());
        verify(notificationService).notifyUser(any(), eq(requester), any(), any(), any());
        verify(notificationService).notifyUser(any(), eq(resolver), any(), any(), any());
    }

    @Test
    void quemResolveuIgualAoTecnico_naoDuplica() {
        order(technician);
        when(userRepository.findById(2L)).thenReturn(Optional.of(technician));

        listener.onServiceOrderEvent(new ServiceOrderEvent(10L, ServiceOrderEventType.RESOLVED, ServiceOrderStatus.RESOLVED, 2L));

        verify(notificationService, times(2)).notifyUser(any(), any(), any(), any(), any());
    }

    @Test
    void eventoSemAtor_naoConsultaUsuarios() {
        order(technician);

        listener.onServiceOrderEvent(new ServiceOrderEvent(10L, ServiceOrderEventType.CREATED, ServiceOrderStatus.OPEN));

        verifyNoInteractions(userRepository);
    }

    @Test
    void conclusao_convidaSoOSolicitanteComTelefoneAAvaliar() {
        requester.setPhone("558899999999");
        requester.setActive(true);
        technician.setActive(true);
        order(technician);

        listener.onServiceOrderEvent(new ServiceOrderEvent(10L, ServiceOrderEventType.RESOLVED, ServiceOrderStatus.RESOLVED));

        ArgumentCaptor<String> toRequester = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> toTechnician = ArgumentCaptor.forClass(String.class);
        verify(notificationService).notifyUser(any(), eq(requester), any(), toRequester.capture(), any());
        verify(notificationService).notifyUser(any(), eq(technician), any(), toTechnician.capture(), any());
        assertTrue(toRequester.getValue().contains("Nota #10"));
        assertFalse(toTechnician.getValue().contains("Nota #10"));
    }

    @Test
    void conclusao_solicitanteSemTelefoneNaoRecebeConvite() {
        requester.setActive(true);
        order(null);

        listener.onServiceOrderEvent(new ServiceOrderEvent(10L, ServiceOrderEventType.RESOLVED, ServiceOrderStatus.RESOLVED));

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(notificationService).notifyUser(any(), eq(requester), any(), message.capture(), any());
        assertFalse(message.getValue().contains("Nota #"));
    }
}
