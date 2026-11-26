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
        doThrow(new RuntimeException("timeout")).doReturn(null).when(whatsapp).send(any(), any(), any(), any());

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

        // 401 não é transitório: uma única tentativa no WhatsApp e segue direto para o e-mail.
        verify(whatsapp, times(1)).send(any(), any(), any(), any());
        List<Notification> records = saved(2);
        assertEquals(corpoDeErro, records.get(0).getErrorMessage());
        assertEquals(NotificationChannel.EMAIL, records.get(1).getChannel());
    }

    @Test
    void falhaHttp429_eTransitoria_eRetenta() {
        RestClientResponseException exception = new RestClientResponseException(
                "429 Too Many Requests", 429, "Too Many Requests", new HttpHeaders(), new byte[0], StandardCharsets.UTF_8);
        doThrow(exception).doReturn("wamid.ok").when(whatsapp).send(any(), any(), any(), any());

        assertTrue(service(true, whatsapp, email).notifyUser(order, user, "s", "m", ServiceOrderStatus.OPEN));

        verify(whatsapp, times(2)).send(any(), any(), any(), any());
        verify(email, never()).send(any(), any(), any(), any());
    }

    @Test
    void whatsappSucesso_guardaWamidEConteudoParaFallbackAssincrono() {
        when(whatsapp.send(any(), any(), any(), any())).thenReturn("wamid.123");

        service(true, whatsapp, email).notifyUser(order, user, "Assunto", "Mensagem", ServiceOrderStatus.OPEN);

        Notification record = saved(1).get(0);
        assertEquals("wamid.123", record.getProviderMessageId());
        assertEquals("Assunto", record.getSubject());
        assertEquals("Mensagem", record.getMessageBody());
    }

    @Test
    void falhaAssincrona_marcaWhatsAppComoFalhaEReenviaPorEmail() {
        order.setStatus(ServiceOrderStatus.IN_PROGRESS);
        Notification enviada = Notification.builder().id(9L).serviceOrder(order).recipient(user)
                .channel(NotificationChannel.WHATSAPP).deliveryStatus(NotificationStatus.SENT).attempt(1)
                .providerMessageId("wamid.123").subject("Assunto").messageBody("Mensagem").build();
        when(repository.findByProviderMessageIdFetchingOrderAndRecipient("wamid.123", NotificationChannel.WHATSAPP))
                .thenReturn(List.of(enviada));

        service(true, whatsapp, email).handleAsyncDeliveryFailure("wamid.123", "131047 Re-engagement message");

        assertEquals(NotificationStatus.FAILED, enviada.getDeliveryStatus());
        assertTrue(enviada.getErrorMessage().contains("131047"));
        verify(email).send(user, "Assunto", "Mensagem", ServiceOrderStatus.IN_PROGRESS);
        List<Notification> records = saved(2);
        assertEquals(NotificationChannel.EMAIL, records.get(1).getChannel());
        assertEquals(NotificationStatus.SENT, records.get(1).getDeliveryStatus());
    }

    @Test
    void falhaAssincronaRepetida_naoReenviaDeNovo() {
        Notification jaFalhou = Notification.builder().serviceOrder(order).recipient(user)
                .channel(NotificationChannel.WHATSAPP).deliveryStatus(NotificationStatus.FAILED).attempt(1)
                .providerMessageId("wamid.123").messageBody("Mensagem").build();
        when(repository.findByProviderMessageIdFetchingOrderAndRecipient("wamid.123", NotificationChannel.WHATSAPP))
                .thenReturn(List.of(jaFalhou));

        service(true, whatsapp, email).handleAsyncDeliveryFailure("wamid.123", "erro");

        verify(email, never()).send(any(), any(), any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    void falhaAssincronaDeMensagemDesconhecida_eIgnorada() {
        when(repository.findByProviderMessageIdFetchingOrderAndRecipient(any(), any())).thenReturn(List.of());

        service(true, whatsapp, email).handleAsyncDeliveryFailure("wamid.resposta-automatica", "erro");

        verify(email, never()).send(any(), any(), any(), any());
        verify(repository, never()).save(any());
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
