package br.com.fpo.sgc.domain.notification;

import br.com.fpo.sgc.domain.serviceorder.ServiceOrder;
import br.com.fpo.sgc.domain.serviceorder.ServiceOrderStatus;
import br.com.fpo.sgc.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationServiceImplTest {

    private NotificationRepository repository;
    private NotificationSender whatsapp;
    private NotificationSender email;
    private ServiceOrder order;
    private User user;

    @BeforeEach
    void setUp() {
        repository = mock(NotificationRepository.class);
        whatsapp = sender(NotificationChannel.WHATSAPP);
        email = sender(NotificationChannel.EMAIL);
        order = ServiceOrder.builder().id(1L).build();
        user = User.builder().id(2L).email("a@b.com").phone("88999999999").build();
    }

    private NotificationSender sender(NotificationChannel channel) {
        NotificationSender s = mock(NotificationSender.class);
        when(s.channel()).thenReturn(channel);
        when(s.canSend(any())).thenReturn(true);
        return s;
    }

    private NotificationServiceImpl service(boolean fallback, NotificationSender... senders) {
        return new NotificationServiceImpl(List.of(senders), repository, fallback, 3, 0);
    }

    private List<Notification> saved(int times) {
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository, times(times)).save(captor.capture());
        return captor.getAllValues();
    }

    @Test
    void whatsappSucesso_naoAcionaEmail() {
        assertTrue(service(true, whatsapp, email).notifyUser(order, user, "s", "m", ServiceOrderStatus.OPEN));

        verify(email, never()).send(any(), any(), any(), any());
        List<Notification> records = saved(1);
        assertEquals(NotificationChannel.WHATSAPP, records.get(0).getChannel());
        assertEquals(NotificationStatus.SENT, records.get(0).getDeliveryStatus());
    }

    @Test
    void whatsappFalhaSempre_retentaEDepoisUsaEmail() {
        doThrow(new RuntimeException("token expirado")).when(whatsapp).send(any(), any(), any(), any());

        assertTrue(service(true, whatsapp, email).notifyUser(order, user, "s", "m", ServiceOrderStatus.OPEN));

        verify(whatsapp, times(3)).send(any(), any(), any(), any());
        verify(email, times(1)).send(any(), any(), any(), any());
        List<Notification> records = saved(4);
        assertEquals(3, records.stream().filter(n -> n.getChannel() == NotificationChannel.WHATSAPP
                && n.getDeliveryStatus() == NotificationStatus.FAILED).count());
        assertEquals("token expirado", records.get(0).getErrorMessage());
        assertEquals(NotificationChannel.EMAIL, records.get(3).getChannel());
        assertEquals(NotificationStatus.SENT, records.get(3).getDeliveryStatus());
    }

    @Test
    void whatsappFalhaUmaVez_recuperaNaRetentativaSemEmail() {
        doThrow(new RuntimeException("timeout")).doNothing().when(whatsapp).send(any(), any(), any(), any());

        assertTrue(service(true, whatsapp, email).notifyUser(order, user, "s", "m", ServiceOrderStatus.OPEN));

        verify(email, never()).send(any(), any(), any(), any());
        List<Notification> records = saved(2);
        assertEquals(NotificationStatus.FAILED, records.get(0).getDeliveryStatus());
        assertEquals(NotificationStatus.SENT, records.get(1).getDeliveryStatus());
        assertEquals(2, records.get(1).getAttempt());
    }

    @Test
    void falhaHttp_registraCorpoDaRespostaDeErroEmVezDaMensagemGenerica() {
        String corpoDeErro = "{\"error\":{\"message\":\"Access token has expired\",\"code\":190}}";
        RestClientResponseException exception = new RestClientResponseException(
                "401 Unauthorized", HttpStatus.UNAUTHORIZED.value(), "Unauthorized",
                new HttpHeaders(), corpoDeErro.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        doThrow(exception).when(whatsapp).send(any(), any(), any(), any());

        assertTrue(service(true, whatsapp, email).notifyUser(order, user, "s", "m", ServiceOrderStatus.OPEN));

        List<Notification> records = saved(4);
        assertEquals(corpoDeErro, records.get(0).getErrorMessage());
    }

    @Test
    void fallbackDesabilitado_naoUsaEmail() {
        doThrow(new RuntimeException("falha")).when(whatsapp).send(any(), any(), any(), any());

        assertFalse(service(false, whatsapp, email).notifyUser(order, user, "s", "m", ServiceOrderStatus.OPEN));

        verify(email, never()).send(any(), any(), any(), any());
        saved(3);
    }

    @Test
    void semTelefone_vaiDiretoParaEmail() {
        when(whatsapp.canSend(user)).thenReturn(false);

        assertTrue(service(true, whatsapp, email).notifyUser(order, user, "s", "m", ServiceOrderStatus.OPEN));

        verify(whatsapp, never()).send(any(), any(), any(), any());
        assertEquals(NotificationChannel.EMAIL, saved(1).get(0).getChannel());
    }

    @Test
    void nenhumCanalDisponivel_retornaFalsoSemRegistros() {
        assertFalse(service(true).notifyUser(order, user, "s", "m", ServiceOrderStatus.OPEN));

        verify(repository, never()).save(any());
    }
}
