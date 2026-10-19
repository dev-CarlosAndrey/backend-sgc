package br.com.fpo.sgc.domain.report;

import br.com.fpo.sgc.domain.notification.Notification;
import br.com.fpo.sgc.domain.notification.NotificationChannel;
import br.com.fpo.sgc.domain.notification.NotificationRepository;
import br.com.fpo.sgc.domain.notification.NotificationStatus;
import br.com.fpo.sgc.domain.report.ReportFilters.Period;
import br.com.fpo.sgc.domain.report.dto.ChannelReportDTO;
import br.com.fpo.sgc.domain.report.dto.ChannelRowDTO;
import br.com.fpo.sgc.domain.report.dto.LabelCountDTO;
import br.com.fpo.sgc.domain.report.dto.NotificationReportDTO;
import br.com.fpo.sgc.domain.serviceorder.ServiceOrder;
import br.com.fpo.sgc.domain.serviceorder.ServiceOrderChannel;
import br.com.fpo.sgc.domain.serviceorder.ServiceOrderRepository;
import br.com.fpo.sgc.domain.serviceorder.ServiceOrderStatus;
import br.com.fpo.sgc.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static br.com.fpo.sgc.domain.notification.NotificationChannel.EMAIL;
import static br.com.fpo.sgc.domain.notification.NotificationChannel.WHATSAPP;
import static br.com.fpo.sgc.domain.notification.NotificationStatus.FAILED;
import static br.com.fpo.sgc.domain.notification.NotificationStatus.SENT;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChannelInsightsServiceImplTest {

    private ServiceOrderRepository serviceOrderRepository;
    private NotificationRepository notificationRepository;
    private ChannelInsightsServiceImpl service;

    @BeforeEach
    void setUp() {
        serviceOrderRepository = mock(ServiceOrderRepository.class);
        notificationRepository = mock(NotificationRepository.class);
        service = new ChannelInsightsServiceImpl(serviceOrderRepository, notificationRepository);
    }

    private static ServiceOrder order(long id, ServiceOrderChannel channel, ServiceOrderStatus status,
                                      String opened, String closed) {
        ServiceOrder so = ServiceOrder.builder().id(id).channel(channel).status(status)
                .closedAt(closed != null ? Instant.parse(closed) : null).build();
        so.setCreatedAt(Instant.parse(opened));
        return so;
    }

    private static Notification attempt(long orderId, long userId, NotificationChannel channel,
                                        NotificationStatus status, int attempt, String error) {
        return Notification.builder()
                .serviceOrder(ServiceOrder.builder().id(orderId).build())
                .recipient(User.builder().id(userId).build())
                .channel(channel).deliveryStatus(status).attempt(attempt).errorMessage(error).build();
    }

    private static List<Notification> whatsAppFailedThenEmail(long orderId, long userId, String error) {
        List<Notification> list = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            list.add(attempt(orderId, userId, WHATSAPP, FAILED, i, error));
        }
        list.add(attempt(orderId, userId, EMAIL, SENT, 1, null));
        return list;
    }

    private static long outcome(NotificationReportDTO report, String key) {
        return report.byOutcome().stream().filter(o -> o.key().equals(key)).mapToLong(LabelCountDTO::total).sum();
    }

    // ---------- OS por canal ----------

    @Test
    void canais_contaParticipacaoStatusETempoMedioPorCanal() {
        when(serviceOrderRepository.findForReport(any(), any(), any(), any(), any(), any(), any())).thenReturn(List.of(
                order(1, ServiceOrderChannel.WHATSAPP, ServiceOrderStatus.RESOLVED, "2026-07-10T12:00:00Z", "2026-07-11T12:00:00Z"),
                order(2, ServiceOrderChannel.WHATSAPP, ServiceOrderStatus.RESOLVED, "2026-07-12T12:00:00Z", "2026-07-12T14:00:00Z"),
                order(3, ServiceOrderChannel.WHATSAPP, ServiceOrderStatus.OPEN, "2026-08-01T12:00:00Z", null),
                order(4, ServiceOrderChannel.PANEL, ServiceOrderStatus.CANCELLED, "2026-08-02T12:00:00Z", "2026-08-03T12:00:00Z"),
                // OS sem canal (anterior à V13) conta como painel
                order(5, null, ServiceOrderStatus.IN_PROGRESS, "2026-08-05T12:00:00Z", null)));

        ChannelReportDTO report = service.channels(new Period(null, null, null));

        assertEquals(5, report.total());
        ChannelRowDTO panel = report.channels().get(0);
        ChannelRowDTO whatsapp = report.channels().get(1);
        assertEquals("PANEL", panel.channel());
        assertEquals(2, panel.total());
        assertEquals(40.0, panel.sharePercent());
        assertEquals(1, panel.cancelled());
        assertEquals(1, panel.pending());
        assertNull(panel.avgResolutionHours());

        assertEquals(3, whatsapp.total());
        assertEquals(60.0, whatsapp.sharePercent());
        assertEquals(2, whatsapp.resolved());
        assertEquals(1, whatsapp.pending());
        assertEquals(13.0, whatsapp.avgResolutionHours()); // (24h + 2h) / 2

        assertEquals(2, report.months().size());
        assertEquals("2026-07", report.months().get(0).month());
        assertEquals(2, report.months().get(0).whatsapp());
        assertEquals(100.0, report.months().get(0).whatsappPercent());
        assertEquals(2, report.months().get(1).panel());
        assertEquals(1, report.months().get(1).whatsapp());
    }

    @Test
    void canais_semOsNoPeriodoNaoDivideporZero() {
        when(serviceOrderRepository.findForReport(any(), any(), any(), any(), any(), any(), any())).thenReturn(List.of());

        ChannelReportDTO report = service.channels(new Period(null, null, null));

        assertEquals(0, report.total());
        assertEquals(2, report.channels().size());
        assertNull(report.channels().get(0).sharePercent());
        assertTrue(report.months().isEmpty());
    }

    @Test
    void canais_csvTemCabecalhoELinhaPorCanal() {
        when(serviceOrderRepository.findForReport(any(), any(), any(), any(), any(), any(), any())).thenReturn(List.of(
                order(1, ServiceOrderChannel.WHATSAPP, ServiceOrderStatus.OPEN, "2026-07-10T12:00:00Z", null)));

        String csv = new String(service.channelsCsv(new Period(null, null, null)), StandardCharsets.UTF_8);

        assertTrue(csv.contains("Tipo;Item;Quantidade"));
        assertTrue(csv.contains("Canal;WhatsApp;1;100,0"));
        assertTrue(csv.contains("Mês;2026-07 - WhatsApp;1;100,0"));
    }

    @Test
    void canais_pdfGeraArquivo() {
        when(serviceOrderRepository.findForReport(any(), any(), any(), any(), any(), any(), any())).thenReturn(List.of());
        byte[] pdf = service.channelsPdf(new Period(null, null, null));
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
    }

    // ---------- reagrupamento das tentativas ----------

    @Test
    void agrupamento_fallbackParaEmailFicaNaMesmaNotificacao() {
        List<List<Notification>> groups = ChannelInsightsServiceImpl.groupIntoDeliveries(
                whatsAppFailedThenEmail(1, 10, "erro"));
        assertEquals(1, groups.size());
        assertEquals(4, groups.get(0).size());
    }

    @Test
    void agrupamento_eventosSeguidosDoMesmoDestinatarioViramNotificacoesSeparadas() {
        List<Notification> attempts = new ArrayList<>();
        // evento 1: WhatsApp na 1ª; evento 2: falha e fallback; evento 3: WhatsApp na 2ª
        attempts.add(attempt(1, 10, WHATSAPP, SENT, 1, null));
        attempts.addAll(whatsAppFailedThenEmail(1, 10, "erro"));
        attempts.add(attempt(1, 10, WHATSAPP, FAILED, 1, "timeout"));
        attempts.add(attempt(1, 10, WHATSAPP, SENT, 2, null));

        List<List<Notification>> groups = ChannelInsightsServiceImpl.groupIntoDeliveries(attempts);

        assertEquals(3, groups.size());
        assertEquals(1, groups.get(0).size());
        assertEquals(4, groups.get(1).size());
        assertEquals(2, groups.get(2).size());
    }

    @Test
    void agrupamento_destinatarioSemTelefoneTemUmaNotificacaoPorEmail() {
        List<List<Notification>> groups = ChannelInsightsServiceImpl.groupIntoDeliveries(List.of(
                attempt(1, 20, EMAIL, SENT, 1, null),
                attempt(1, 20, EMAIL, SENT, 1, null),
                attempt(2, 20, EMAIL, SENT, 1, null)));
        assertEquals(3, groups.size());
    }

    // ---------- relatório de notificações ----------

    @Test
    void notificacoes_calculaDesfechosTaxasEErros() {
        String windowError = "{\"error\":{\"message\":\"(#131047) Re-engagement message\",\"code\":131047}}";
        List<Notification> attempts = new ArrayList<>();
        attempts.add(attempt(1, 10, WHATSAPP, SENT, 1, null));                 // WhatsApp na 1ª
        attempts.add(attempt(1, 11, WHATSAPP, FAILED, 1, "timeout"));          // WhatsApp após retentativa
        attempts.add(attempt(1, 11, WHATSAPP, SENT, 2, null));
        attempts.addAll(whatsAppFailedThenEmail(2, 10, windowError));          // fallback
        attempts.add(attempt(2, 20, EMAIL, SENT, 1, null));                    // só e-mail
        attempts.add(attempt(3, 10, WHATSAPP, FAILED, 1, windowError));        // não entregue (sem fallback)
        when(notificationRepository.findForReport(any(), any(), any())).thenReturn(attempts);

        NotificationReportDTO report = service.notifications(new Period(null, null, null));

        assertEquals(9, report.totalAttempts());
        assertEquals(5, report.totalNotifications());
        assertEquals(4, report.delivered());
        assertEquals(80.0, report.deliveryRatePercent());
        // 4 notificações tentaram WhatsApp; 1 caiu para o e-mail
        assertEquals(25.0, report.fallbackRatePercent());

        assertEquals(1, outcome(report, "WHATSAPP_FIRST_ATTEMPT"));
        assertEquals(1, outcome(report, "WHATSAPP_AFTER_RETRY"));
        assertEquals(1, outcome(report, "EMAIL_FALLBACK"));
        assertEquals(1, outcome(report, "EMAIL_ONLY"));
        assertEquals(1, outcome(report, "NOT_DELIVERED"));

        var whatsapp = report.channels().get(0);
        assertEquals("WHATSAPP", whatsapp.channel());
        assertEquals(7, whatsapp.attempts());
        assertEquals(2, whatsapp.sent());
        assertEquals(5, whatsapp.failed());
        assertEquals(28.6, whatsapp.successRatePercent()); // 2 de 7
        assertEquals(100.0, report.channels().get(1).successRatePercent());

        // Erro em JSON da Graph API é reduzido ao campo "message"
        assertEquals("(#131047) Re-engagement message", report.topErrors().get(0).key());
        assertEquals(4, report.topErrors().get(0).total());
        assertEquals("timeout", report.topErrors().get(1).key());
    }

    @Test
    void notificacoes_semTentativasRetornaTaxasNulas() {
        when(notificationRepository.findForReport(any(), any(), any())).thenReturn(List.of());

        NotificationReportDTO report = service.notifications(new Period(null, null, null));

        assertEquals(0, report.totalNotifications());
        assertNull(report.deliveryRatePercent());
        assertNull(report.fallbackRatePercent());
        assertTrue(report.topErrors().isEmpty());
    }

    @Test
    void notificacoes_csvEPdf() {
        when(notificationRepository.findForReport(any(), any(), any())).thenReturn(whatsAppFailedThenEmail(1, 10, null));

        String csv = new String(service.notificationsCsv(new Period(null, null, null)), StandardCharsets.UTF_8);
        assertTrue(csv.contains("Desfecho;E-mail (fallback);1;100,0"));
        assertTrue(csv.contains("Erro;Erro sem detalhe;3"));

        byte[] pdf = service.notificationsPdf(new Period(null, null, null));
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
    }

    @Test
    void rotuloDeErroCortaMensagensLongas() {
        String label = ChannelInsightsServiceImpl.errorLabel("x".repeat(400));
        assertEquals(153, label.length());
        assertTrue(label.endsWith("..."));
    }
}
