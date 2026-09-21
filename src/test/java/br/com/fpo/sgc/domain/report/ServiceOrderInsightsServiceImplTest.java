package br.com.fpo.sgc.domain.report;

import br.com.fpo.sgc.common.exception.BusinessException;
import br.com.fpo.sgc.domain.report.ReportFilters.Period;
import br.com.fpo.sgc.domain.report.dto.MonthlyServiceOrdersReportDTO;
import br.com.fpo.sgc.domain.report.dto.StageTimeReportDTO;
import br.com.fpo.sgc.domain.serviceorder.*;
import br.com.fpo.sgc.domain.serviceorder.statuslog.ServiceOrderStatusLog;
import br.com.fpo.sgc.domain.serviceorder.statuslog.ServiceOrderStatusLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServiceOrderInsightsServiceImplTest {

    private ServiceOrderRepository serviceOrderRepository;
    private ServiceOrderStatusLogRepository statusLogRepository;
    private ServiceOrderInsightsServiceImpl service;

    @BeforeEach
    void setUp() {
        serviceOrderRepository = mock(ServiceOrderRepository.class);
        statusLogRepository = mock(ServiceOrderStatusLogRepository.class);
        service = new ServiceOrderInsightsServiceImpl(serviceOrderRepository, statusLogRepository);
    }

    private static ServiceOrder order(long id, ServiceOrderPriority priority, String opened, String closed, String cost) {
        ServiceOrder so = ServiceOrder.builder().id(id).priority(priority)
                .status(closed != null ? ServiceOrderStatus.RESOLVED : ServiceOrderStatus.OPEN)
                .closedAt(closed != null ? Instant.parse(closed) : null)
                .cost(cost != null ? new BigDecimal(cost) : null).build();
        so.setCreatedAt(Instant.parse(opened));
        return so;
    }

    private static ServiceOrderStatusLog log(ServiceOrder so, ServiceOrderStatus status, String at) {
        ServiceOrderStatusLog log = ServiceOrderStatusLog.builder().serviceOrder(so).newStatus(status).build();
        log.setCreatedAt(Instant.parse(at));
        return log;
    }

    // ---------- mensal ----------

    @Test
    void mensal_agrupaPorMesComZerosNosMesesVazios() {
        // fuso America/Fortaleza (UTC-3): 01/07 01:00Z ainda é 30/06 local
        when(serviceOrderRepository.findForReport(any(), any(), eq(null), eq(null), any(), eq(null), any())).thenReturn(List.of(
                order(1, ServiceOrderPriority.LOW, "2026-07-10T12:00:00Z", null, null),
                order(2, ServiceOrderPriority.LOW, "2026-07-20T12:00:00Z", null, null),
                order(3, ServiceOrderPriority.LOW, "2026-09-05T12:00:00Z", null, null),
                order(4, ServiceOrderPriority.LOW, "2026-07-01T01:00:00Z", null, null)));
        when(serviceOrderRepository.findForReport(eq(null), eq(null), any(), any(), any(), eq(ServiceOrderStatus.RESOLVED), any())).thenReturn(List.of(
                order(1, ServiceOrderPriority.LOW, "2026-07-10T12:00:00Z", "2026-07-11T12:00:00Z", "100.00"),
                order(2, ServiceOrderPriority.LOW, "2026-07-20T12:00:00Z", "2026-09-02T12:00:00Z", "50.50")));

        MonthlyServiceOrdersReportDTO report = service.monthly(
                new Period(LocalDate.of(2026, 6, 15), LocalDate.of(2026, 9, 10), null));

        assertEquals(4, report.months().size());
        assertEquals("2026-06", report.months().get(0).month());
        assertEquals(1, report.months().get(0).opened()); // OS 4 (30/06 local)
        assertEquals(2, report.months().get(1).opened());
        assertEquals(1, report.months().get(1).resolved());
        assertEquals(new BigDecimal("100.00"), report.months().get(1).cost());
        // agosto sem movimento
        assertEquals(0, report.months().get(2).opened());
        assertEquals(BigDecimal.ZERO, report.months().get(2).cost());
        assertEquals(new BigDecimal("50.50"), report.months().get(3).cost());
        assertEquals(4, report.totalOpened());
        assertEquals(2, report.totalResolved());
    }

    @Test
    void mensal_semFiltro_traz12Meses() {
        when(serviceOrderRepository.findForReport(any(), any(), any(), any(), any(), any(), any())).thenReturn(List.of());

        assertEquals(12, service.monthly(new Period(null, null, null)).months().size());
    }

    @Test
    void mensal_periodoMaiorQue60Meses_ouInvertido_lancaBusinessException() {
        assertThrows(BusinessException.class, () -> service.monthly(
                new Period(LocalDate.of(2020, 1, 1), LocalDate.of(2026, 1, 1), null)));
        assertThrows(BusinessException.class, () -> service.monthly(
                new Period(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 1, 1), null)));
    }

    @Test
    void csvMensal_temCabecalhoEValoresPtBr() {
        when(serviceOrderRepository.findForReport(any(), any(), any(), any(), any(), any(), any())).thenReturn(List.of());

        String csv = new String(service.monthlyCsv(new Period(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), null)),
                StandardCharsets.UTF_8);

        assertTrue(csv.contains("Mês;Abertas;Concluídas;Custo das concluídas"));
        assertTrue(csv.contains("2026-09;0;0;0,00"));
    }

    // ---------- tempo por etapa ----------

    @Test
    void tempoPorEtapa_medeCadaEtapaEIdentificaGargalo() {
        ServiceOrder a = order(1, ServiceOrderPriority.HIGH, "2026-09-01T10:00:00Z", null, null);
        ServiceOrder b = order(2, ServiceOrderPriority.HIGH, "2026-09-02T10:00:00Z", null, null);
        ServiceOrder c = order(3, ServiceOrderPriority.LOW, "2026-09-03T10:00:00Z", null, null);
        when(statusLogRepository.findForReport(any(), any(), any())).thenReturn(List.of(
                // A: 1h em OPEN, 5h em IN_PROGRESS, resolvida (6h no total)
                log(a, ServiceOrderStatus.OPEN, "2026-09-01T10:00:00Z"),
                log(a, ServiceOrderStatus.IN_PROGRESS, "2026-09-01T11:00:00Z"),
                log(a, ServiceOrderStatus.RESOLVED, "2026-09-01T16:00:00Z"),
                // B: 3h em OPEN, 1h em IN_PROGRESS, resolvida (4h)
                log(b, ServiceOrderStatus.OPEN, "2026-09-02T10:00:00Z"),
                log(b, ServiceOrderStatus.IN_PROGRESS, "2026-09-02T13:00:00Z"),
                log(b, ServiceOrderStatus.RESOLVED, "2026-09-02T14:00:00Z"),
                // C: ainda em OPEN, sem intervalo fechado
                log(c, ServiceOrderStatus.OPEN, "2026-09-03T10:00:00Z")));

        StageTimeReportDTO report = service.stageTimes(new Period(null, null, null));

        assertEquals(3, report.orders());
        var open = report.stages().stream().filter(s -> s.status().equals("OPEN")).findFirst().orElseThrow();
        assertEquals(2, open.samples());
        assertEquals(2.0, open.avgHours()); // (1h + 3h) / 2
        assertEquals(3.0, open.maxHours());
        var progress = report.stages().stream().filter(s -> s.status().equals("IN_PROGRESS")).findFirst().orElseThrow();
        assertEquals(3.0, progress.avgHours()); // (5h + 1h) / 2
        assertEquals(2, report.stages().size()); // estados finais não têm etapa
        // empate em 2h vs 3h: IN_PROGRESS é o maior
        assertEquals("IN_PROGRESS", report.bottleneck());

        var high = report.byPriority().stream().filter(p -> p.priority().equals("HIGH")).findFirst().orElseThrow();
        assertEquals(2, high.orders());
        assertEquals(2.0, high.avgFirstResponseHours());
        assertEquals(5.0, high.avgResolutionHours()); // (6h + 4h) / 2
        var low = report.byPriority().stream().filter(p -> p.priority().equals("LOW")).findFirst().orElseThrow();
        assertEquals(1, low.orders());
        assertNull(low.avgFirstResponseHours());
        assertNull(low.avgResolutionHours());
        var urgent = report.byPriority().stream().filter(p -> p.priority().equals("URGENT")).findFirst().orElseThrow();
        assertEquals(0, urgent.orders());
    }

    @Test
    void tempoPorEtapa_semHistorico_retornaZerosENulos() {
        when(statusLogRepository.findForReport(any(), any(), any())).thenReturn(List.of());

        StageTimeReportDTO report = service.stageTimes(new Period(null, null, null));

        assertEquals(0, report.orders());
        assertNull(report.bottleneck());
        assertNull(report.stages().get(0).avgHours());
    }
}
