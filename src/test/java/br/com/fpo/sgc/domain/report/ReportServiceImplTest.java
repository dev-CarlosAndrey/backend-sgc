package br.com.fpo.sgc.domain.report;

import br.com.fpo.sgc.domain.asset.Asset;
import br.com.fpo.sgc.domain.asset.AssetRepository;
import br.com.fpo.sgc.domain.asset.AssetStatus;
import br.com.fpo.sgc.domain.asset.category.AssetCategory;
import br.com.fpo.sgc.common.exception.BusinessException;
import br.com.fpo.sgc.domain.report.dto.MaintenanceCostReportDTO;
import br.com.fpo.sgc.domain.location.sector.Sector;
import br.com.fpo.sgc.domain.report.dto.AssetReportDTO;
import br.com.fpo.sgc.domain.report.dto.ServiceOrderReportDTO;
import br.com.fpo.sgc.domain.serviceorder.*;
import br.com.fpo.sgc.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReportServiceImplTest {

    private AssetRepository assetRepository;
    private ServiceOrderRepository serviceOrderRepository;
    private ReportServiceImpl service;

    @BeforeEach
    void setUp() {
        assetRepository = mock(AssetRepository.class);
        serviceOrderRepository = mock(ServiceOrderRepository.class);
        service = new ReportServiceImpl(assetRepository, serviceOrderRepository);
    }

    private static final ServiceOrderReportFilter ALL = new ServiceOrderReportFilter(null, null, ServiceOrderPeriodField.OPENED, null, null, null);

    private ServiceOrder order(long id, ServiceOrderStatus status, ServiceOrderPriority priority, User technician,
                               Asset asset, String opened, String closed, String cost) {
        ServiceOrder so = ServiceOrder.builder().id(id).title("OS " + id).status(status).priority(priority)
                .channel(ServiceOrderChannel.PANEL)
                .requester(User.builder().id(1L).name("Prof").build())
                .sector(Sector.builder().name("Setor A").build())
                .technician(technician).asset(asset)
                .closedAt(closed != null ? Instant.parse(closed) : null)
                .cost(cost != null ? new BigDecimal(cost) : null)
                .build();
        so.setCreatedAt(Instant.parse(opened));
        return so;
    }

    // ---------- RF19 ----------

    @Test
    void relatorioDeBens_agrupaPorCategoriaESituacaoComZeros() {
        when(assetRepository.countByCategoryAndStatus(null)).thenReturn(List.<Object[]>of(
                new Object[]{1L, "Informática", AssetStatus.IN_USE, 5L},
                new Object[]{1L, "Informática", AssetStatus.IN_MAINTENANCE, 2L},
                new Object[]{null, null, AssetStatus.AVAILABLE, 1L},
                new Object[]{2L, "Mobiliário", AssetStatus.IN_USE, 3L}));

        AssetReportDTO report = service.assetsReport(null);

        assertEquals(11, report.total());
        assertEquals(3, report.byCategory().size());
        assertEquals("Informática", report.byCategory().get(0).categoryName());
        assertEquals(7, report.byCategory().get(0).total());
        assertEquals(0L, report.byCategory().get(0).byStatus().get("DISCARDED"));
        assertEquals(2L, report.byCategory().get(0).byStatus().get("IN_MAINTENANCE"));
        // bens sem categoria aparecem com nome próprio e id nulo
        var semCategoria = report.byCategory().stream().filter(r -> r.categoryId() == null).findFirst().orElseThrow();
        assertEquals("Sem categoria", semCategoria.categoryName());
        assertEquals(4, report.byStatus().size());
        assertEquals(8L, report.byStatus().stream().filter(s -> s.key().equals("IN_USE")).findFirst().orElseThrow().total());
    }

    @Test
    void csvDeBens_temCabecalhoEmPortuguesETotalGeral() {
        when(assetRepository.countByCategoryAndStatus(null)).thenReturn(List.<Object[]>of(
                new Object[]{1L, "Informática", AssetStatus.IN_USE, 5L}));

        String csv = new String(service.assetsReportCsv(null), StandardCharsets.UTF_8);

        assertTrue(csv.contains("Categoria;Total;Disponível;Em uso;Em manutenção;Baixado"));
        assertTrue(csv.contains("Informática;5;0;5;0;0"));
        assertTrue(csv.contains("Total geral;5;0;5;0;0"));
    }

    // ---------- RF20 ----------

    @Test
    void relatorioDeOs_agregaPorStatusPrioridadeTecnicoETempoMedio() {
        User ana = User.builder().id(10L).name("Ana").build();
        Asset projetor = Asset.builder().id(100L).tagNumber("T-001").name("Projetor").build();
        when(serviceOrderRepository.findForReport(any(), any(), any(), any(), any(), any(), any())).thenReturn(List.of(
                order(1, ServiceOrderStatus.RESOLVED, ServiceOrderPriority.HIGH, ana, projetor,
                        "2026-09-01T10:00:00Z", "2026-09-01T14:00:00Z", "100.50"),
                order(2, ServiceOrderStatus.RESOLVED, ServiceOrderPriority.LOW, ana, projetor,
                        "2026-09-02T10:00:00Z", "2026-09-02T12:00:00Z", "50.00"),
                order(3, ServiceOrderStatus.OPEN, ServiceOrderPriority.HIGH, null, null,
                        "2026-09-03T10:00:00Z", null, null),
                // cancelada não entra no tempo médio de atendimento
                order(4, ServiceOrderStatus.CANCELLED, ServiceOrderPriority.LOW, ana, null,
                        "2026-09-04T10:00:00Z", "2026-09-04T10:30:00Z", null)));

        ServiceOrderReportDTO report = service.serviceOrdersReport(ALL);

        assertEquals(4, report.total());
        assertEquals(2, report.resolved());
        assertEquals(new BigDecimal("150.50"), report.totalCost());
        assertEquals(3.0, report.avgResolutionHours()); // (4h + 2h) / 2
        assertEquals(2L, report.byStatus().stream().filter(s -> s.key().equals("RESOLVED")).findFirst().orElseThrow().total());
        assertEquals(2L, report.byPriority().stream().filter(p -> p.key().equals("HIGH")).findFirst().orElseThrow().total());

        assertEquals(2, report.byTechnician().size());
        var anaRow = report.byTechnician().get(0);
        assertEquals("Ana", anaRow.technicianName());
        assertEquals(3, anaRow.total());
        assertEquals(2, anaRow.resolved());
        assertEquals(3.0, anaRow.avgResolutionHours());
        var semTecnico = report.byTechnician().get(1);
        assertNull(semTecnico.technicianId());
        assertEquals("Sem técnico", semTecnico.technicianName());
        assertNull(semTecnico.avgResolutionHours());

        assertEquals(1, report.topAssets().size());
        assertEquals("T-001", report.topAssets().get(0).tagNumber());
        assertEquals(2, report.topAssets().get(0).total());
    }

    @Test
    void relatorioDeOs_semOrdens_retornaZerosENulos() {
        when(serviceOrderRepository.findForReport(any(), any(), any(), any(), any(), any(), any())).thenReturn(List.of());

        ServiceOrderReportDTO report = service.serviceOrdersReport(ALL);

        assertEquals(0, report.total());
        assertEquals(BigDecimal.ZERO, report.totalCost());
        assertNull(report.avgResolutionHours());
        assertTrue(report.byTechnician().isEmpty());
    }

    @Test
    void filtroPorAbertura_naoPreencheAsPontasDeConclusao_eViceVersa() {
        when(serviceOrderRepository.findForReport(any(), any(), any(), any(), any(), any(), any())).thenReturn(List.of());
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 30);

        service.serviceOrdersReport(new ServiceOrderReportFilter(from, to, ServiceOrderPeriodField.OPENED, 7L, ServiceOrderStatus.OPEN, 3L));
        service.serviceOrdersReport(new ServiceOrderReportFilter(from, to, ServiceOrderPeriodField.CLOSED, null, null, null));

        ArgumentCaptor<Instant> openedFrom = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> closedFrom = ArgumentCaptor.forClass(Instant.class);
        verify(serviceOrderRepository, times(2)).findForReport(openedFrom.capture(), any(), closedFrom.capture(), any(), any(), any(), any());
        assertEquals(Instant.parse("2026-09-01T03:00:00Z"), openedFrom.getAllValues().get(0));
        assertNull(closedFrom.getAllValues().get(0));
        assertNull(openedFrom.getAllValues().get(1));
        assertEquals(Instant.parse("2026-09-01T03:00:00Z"), closedFrom.getAllValues().get(1));
        verify(serviceOrderRepository).findForReport(any(), any(), any(), any(), eq(7L), eq(ServiceOrderStatus.OPEN), eq(3L));
    }

    @Test
    void csvDeOs_formataDatasNoFusoLocalECustoComVirgula() {
        when(serviceOrderRepository.findForReport(any(), any(), any(), any(), any(), any(), any())).thenReturn(List.of(
                order(1, ServiceOrderStatus.RESOLVED, ServiceOrderPriority.HIGH, User.builder().id(10L).name("Ana").build(),
                        Asset.builder().id(100L).tagNumber("T-001").name("Projetor").build(),
                        "2026-09-01T13:00:00Z", "2026-09-01T15:30:00Z", "100.5"),
                order(2, ServiceOrderStatus.OPEN, ServiceOrderPriority.LOW, null, null, "2026-09-02T13:00:00Z", null, null)));

        String csv = new String(service.serviceOrdersCsv(ALL), StandardCharsets.UTF_8);

        assertTrue(csv.contains("Código;Título;Status;Prioridade;Canal;Solicitante;Técnico;Bem (tombamento);Setor;Abertura;Conclusão;Custo"));
        assertTrue(csv.contains("1;OS 1;Concluída;Alta;PANEL;Prof;Ana;T-001;Setor A;01/09/2026 10:00;01/09/2026 12:30;100,50"));
        assertTrue(csv.contains("2;OS 2;Aberta;Baixa;PANEL;Prof;;;Setor A;02/09/2026 10:00;;"));
    }

    // ---------- custo de manutenção × valor do bem ----------

    private static MaintenanceCostFilter costFilter(double threshold) {
        return new MaintenanceCostFilter(null, null, ServiceOrderPeriodField.CLOSED, null, threshold);
    }

    @Test
    void custoPorBem_calculaPercentualSinalizaAcimaDoLimiteEOrdena() {
        AssetCategory info = AssetCategory.builder().name("Informática").build();
        Asset notebook = Asset.builder().id(1L).tagNumber("T-001").name("Notebook").category(info)
                .acquisitionValue(new BigDecimal("2000.00")).build();
        Asset mesa = Asset.builder().id(2L).tagNumber("T-002").name("Mesa")
                .acquisitionValue(new BigDecimal("1000.00")).build();
        Asset semValor = Asset.builder().id(3L).tagNumber("T-003").name("Doado").build();
        when(serviceOrderRepository.findForReport(any(), any(), any(), any(), any(), eq(ServiceOrderStatus.RESOLVED), any()))
                .thenReturn(List.of(
                        order(1, ServiceOrderStatus.RESOLVED, ServiceOrderPriority.HIGH, null, notebook, "2026-09-01T10:00:00Z", "2026-09-01T12:00:00Z", "700.00"),
                        order(2, ServiceOrderStatus.RESOLVED, ServiceOrderPriority.HIGH, null, notebook, "2026-09-02T10:00:00Z", "2026-09-02T12:00:00Z", "500.00"),
                        order(3, ServiceOrderStatus.RESOLVED, ServiceOrderPriority.LOW, null, mesa, "2026-09-03T10:00:00Z", "2026-09-03T12:00:00Z", "100.00"),
                        order(4, ServiceOrderStatus.RESOLVED, ServiceOrderPriority.LOW, null, semValor, "2026-09-04T10:00:00Z", "2026-09-04T12:00:00Z", "80.00"),
                        // sem custo e sem bem: ignoradas
                        order(5, ServiceOrderStatus.RESOLVED, ServiceOrderPriority.LOW, null, mesa, "2026-09-05T10:00:00Z", "2026-09-05T12:00:00Z", null),
                        order(6, ServiceOrderStatus.RESOLVED, ServiceOrderPriority.LOW, null, null, "2026-09-06T10:00:00Z", "2026-09-06T12:00:00Z", "10.00")));

        MaintenanceCostReportDTO report = service.maintenanceCostReport(costFilter(50));

        assertEquals(3, report.assets().size());
        var first = report.assets().get(0);
        assertEquals("T-001", first.tagNumber());
        assertEquals(2, first.orders());
        assertEquals(new BigDecimal("1200.00"), first.maintenanceCost());
        assertEquals(60.0, first.costPercentOfValue());
        assertTrue(first.exceedsThreshold());
        assertEquals("Informática", first.categoryName());
        var second = report.assets().get(1);
        assertEquals("T-002", second.tagNumber());
        assertEquals(10.0, second.costPercentOfValue());
        assertFalse(second.exceedsThreshold());
        assertEquals("Sem categoria", second.categoryName());
        // sem valor de aquisição: percentual nulo, nunca sinalizado, vai para o fim
        var last = report.assets().get(2);
        assertNull(last.costPercentOfValue());
        assertFalse(last.exceedsThreshold());

        assertEquals(new BigDecimal("1380.00"), report.totalMaintenanceCost());
        assertEquals(new BigDecimal("3000.00"), report.totalAcquisitionValue());
        assertEquals(1, report.assetsAboveThreshold());
    }

    @Test
    void custoPorBem_limiteNegativo_lancaBusinessException() {
        assertThrows(BusinessException.class, () -> service.maintenanceCostReport(costFilter(-1)));
    }

    @Test
    void csvDeCustoPorBem_temCabecalhoEValoresNoFormatoPtBr() {
        Asset notebook = Asset.builder().id(1L).tagNumber("T-001").name("Notebook")
                .acquisitionValue(new BigDecimal("2000")).build();
        when(serviceOrderRepository.findForReport(any(), any(), any(), any(), any(), any(), any())).thenReturn(List.of(
                order(1, ServiceOrderStatus.RESOLVED, ServiceOrderPriority.HIGH, null, notebook, "2026-09-01T10:00:00Z", "2026-09-01T12:00:00Z", "1200.5")));

        String csv = new String(service.maintenanceCostCsv(costFilter(50)), StandardCharsets.UTF_8);

        assertTrue(csv.contains("Tombamento;Bem;Categoria;Valor de aquisição;Nº de OS;Custo de manutenção;Custo / valor (%);Candidato a substituição"));
        assertTrue(csv.contains("T-001;Notebook;Sem categoria;2000,00;1;1200,50;60,0;Sim"));
    }
}
