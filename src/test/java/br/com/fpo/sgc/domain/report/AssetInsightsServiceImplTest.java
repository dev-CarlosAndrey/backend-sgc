package br.com.fpo.sgc.domain.report;

import br.com.fpo.sgc.common.exception.BusinessException;
import br.com.fpo.sgc.domain.asset.Asset;
import br.com.fpo.sgc.domain.asset.AssetConservationState;
import br.com.fpo.sgc.domain.asset.AssetRepository;
import br.com.fpo.sgc.domain.asset.AssetStatus;
import br.com.fpo.sgc.domain.asset.category.AssetCategory;
import br.com.fpo.sgc.domain.location.sector.Sector;
import br.com.fpo.sgc.domain.report.ReportFilters.Critical;
import br.com.fpo.sgc.domain.report.ReportFilters.GroupBy;
import br.com.fpo.sgc.domain.report.ReportFilters.Patrimony;
import br.com.fpo.sgc.domain.report.dto.CriticalAssetsReportDTO;
import br.com.fpo.sgc.domain.report.dto.PatrimonyReportDTO;
import br.com.fpo.sgc.domain.serviceorder.ServiceOrder;
import br.com.fpo.sgc.domain.serviceorder.ServiceOrderRepository;
import br.com.fpo.sgc.domain.serviceorder.ServiceOrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AssetInsightsServiceImplTest {

    private static final LocalDate AS_OF = LocalDate.of(2026, 9, 21);

    private AssetRepository assetRepository;
    private ServiceOrderRepository serviceOrderRepository;
    private AssetInsightsServiceImpl service;

    @BeforeEach
    void setUp() {
        assetRepository = mock(AssetRepository.class);
        serviceOrderRepository = mock(ServiceOrderRepository.class);
        service = new AssetInsightsServiceImpl(assetRepository, serviceOrderRepository);
    }

    private static Asset asset(long id, String tag, String category, String sector, String value, LocalDate date,
                               AssetConservationState state) {
        return Asset.builder().id(id).tagNumber(tag).name("Bem " + tag).status(AssetStatus.IN_USE)
                .category(category != null ? AssetCategory.builder().id((long) category.hashCode()).name(category).build() : null)
                .sector(Sector.builder().id(id * 100).name(sector).build())
                .acquisitionValue(value != null ? new BigDecimal(value) : null)
                .acquisitionDate(date).conservationState(state).build();
    }

    private static ServiceOrder order(Asset asset, ServiceOrderStatus status, String cost) {
        return ServiceOrder.builder().asset(asset).status(status)
                .cost(cost != null ? new BigDecimal(cost) : null).build();
    }

    // ---------- patrimônio ----------

    @Test
    void patrimonio_depreciaLinearmenteEAgrupaPorCategoria() {
        Asset meio = asset(1, "T-1", "Informática", "Sala 1", "1000.00", AS_OF.minusYears(5), null);      // 50%
        Asset velho = asset(2, "T-2", "Informática", "Sala 2", "600.00", AS_OF.minusYears(20), null);     // 100%
        Asset novo = asset(3, "T-3", "Informática", "Sala 1", "400.00", AS_OF.plusMonths(1), null);       // futuro: 0%
        Asset semDados = asset(4, "T-4", null, "Sala 1", null, null, null);
        Asset baixado = asset(5, "T-5", "Informática", "Sala 1", "9999.00", AS_OF.minusYears(1), null);
        baixado.setStatus(AssetStatus.DISCARDED);
        when(assetRepository.findForPatrimony(null)).thenReturn(List.of(meio, velho, novo, semDados, baixado));

        PatrimonyReportDTO report = service.patrimony(new Patrimony(null, GroupBy.CATEGORY, 10, AS_OF));

        assertEquals(4, report.assets()); // baixado fora
        assertEquals(1, report.assetsWithoutValuation());
        assertEquals(new BigDecimal("2000.00"), report.acquisitionValue());
        assertEquals(new BigDecimal("1100.00"), report.accumulatedDepreciation()); // 500 + 600 + 0
        assertEquals(new BigDecimal("900.00"), report.bookValue());
        assertEquals(2, report.groups().size());
        var info = report.groups().get(0); // maior valor contábil primeiro
        assertEquals("Informática", info.groupName());
        assertEquals(3, info.assets());
        assertEquals("Sem categoria", report.groups().get(1).groupName());
        assertEquals(1, report.groups().get(1).assetsWithoutValuation());
    }

    @Test
    void patrimonio_agrupaPorSetor() {
        when(assetRepository.findForPatrimony(null)).thenReturn(List.of(
                asset(1, "T-1", "A", "Sala 1", "100.00", AS_OF, null),
                asset(1, "T-2", "B", "Sala 1", "100.00", AS_OF, null)));

        PatrimonyReportDTO report = service.patrimony(new Patrimony(null, GroupBy.SECTOR, 10, AS_OF));

        assertEquals(1, report.groups().size());
        assertEquals("Sala 1", report.groups().get(0).groupName());
        assertEquals(2, report.groups().get(0).assets());
    }

    @Test
    void patrimonio_vidaUtilInvalida_lancaBusinessException() {
        assertThrows(BusinessException.class, () -> service.patrimony(new Patrimony(null, GroupBy.CATEGORY, 0, AS_OF)));
        assertThrows(BusinessException.class, () -> service.patrimony(new Patrimony(null, GroupBy.CATEGORY, 51, AS_OF)));
    }

    @Test
    void csvDePatrimonio_temTotalGeralEValoresPtBr() {
        when(assetRepository.findForPatrimony(null)).thenReturn(List.of(
                asset(1, "T-1", "Informática", "Sala 1", "1000.00", AS_OF.minusYears(5), null)));

        String csv = new String(service.patrimonyCsv(new Patrimony(null, GroupBy.CATEGORY, 10, AS_OF)), StandardCharsets.UTF_8);

        assertTrue(csv.contains("Categoria;Bens;Sem valorização;Valor de aquisição;Depreciação acumulada;Valor contábil"));
        assertTrue(csv.contains("Informática;1;0;1000,00;500,00;500,00"));
        assertTrue(csv.contains("Total geral;1;0;1000,00;500,00;500,00"));
    }

    @Test
    void pdfDePatrimonio_geraPdfValido() {
        when(assetRepository.findForPatrimony(null)).thenReturn(List.of(
                asset(1, "T-1", "Informática", "Sala 1", "1000.00", AS_OF.minusYears(5), null)));

        byte[] pdf = service.patrimonyPdf(new Patrimony(null, GroupBy.CATEGORY, 10, AS_OF));

        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
    }

    // ---------- bens críticos ----------

    @Test
    void criticos_combinaEstadoRuimEManutencaoRecorrente() {
        Asset ruim = asset(1, "T-1", "Info", "Sala 1", null, null, AssetConservationState.BAD);
        Asset recorrente = asset(2, "T-2", "Info", "Sala 1", null, null, AssetConservationState.GOOD);
        Asset ambos = asset(3, "T-3", "Info", "Sala 1", null, null, AssetConservationState.UNSERVICEABLE);
        Asset umaOs = asset(4, "T-4", "Info", "Sala 1", null, null, AssetConservationState.GOOD);
        Asset baixado = asset(5, "T-5", "Info", "Sala 1", null, null, AssetConservationState.BAD);
        baixado.setStatus(AssetStatus.DISCARDED);
        when(assetRepository.findByConservationStates(any(), any())).thenReturn(List.of(ruim, ambos, baixado));
        when(serviceOrderRepository.findForReport(any(), any(), any(), any(), any(), any(), any())).thenReturn(List.of(
                order(recorrente, ServiceOrderStatus.RESOLVED, "100.00"),
                order(recorrente, ServiceOrderStatus.OPEN, null),
                order(ambos, ServiceOrderStatus.RESOLVED, "30.00"),
                order(ambos, ServiceOrderStatus.RESOLVED, "20.00"),
                order(ambos, ServiceOrderStatus.CANCELLED, "999.00"), // cancelada não conta
                order(umaOs, ServiceOrderStatus.RESOLVED, "10.00"),
                order(null, ServiceOrderStatus.OPEN, null)));

        CriticalAssetsReportDTO report = service.criticalAssets(new Critical(null, null, null, 2));

        assertEquals(3, report.total()); // umaOs e baixado ficam de fora
        assertEquals("T-3", report.assets().get(0).tagNumber()); // os dois motivos primeiro
        assertEquals(List.of("BAD_CONSERVATION", "RECURRENT_MAINTENANCE"), report.assets().get(0).reasons());
        assertEquals(2, report.assets().get(0).orders());
        assertEquals(new BigDecimal("50.00"), report.assets().get(0).maintenanceCost());
        assertEquals("T-2", report.assets().get(1).tagNumber());
        assertEquals(List.of("RECURRENT_MAINTENANCE"), report.assets().get(1).reasons());
        assertEquals(new BigDecimal("100.00"), report.assets().get(1).maintenanceCost());
        assertEquals("T-1", report.assets().get(2).tagNumber());
        assertEquals(0, report.assets().get(2).orders());
        assertEquals(List.of("BAD_CONSERVATION"), report.assets().get(2).reasons());
    }

    @Test
    void criticos_minimoInvalido_lancaBusinessException() {
        assertThrows(BusinessException.class, () -> service.criticalAssets(new Critical(null, null, null, 0)));
    }

    @Test
    void csvDeCriticos_temMotivoEmPortugues() {
        when(assetRepository.findByConservationStates(any(), any())).thenReturn(List.of(
                asset(1, "T-1", "Info", "Sala 1", null, null, AssetConservationState.BAD)));
        when(serviceOrderRepository.findForReport(any(), any(), any(), any(), any(), any(), any())).thenReturn(List.of());

        String csv = new String(service.criticalAssetsCsv(new Critical(null, null, null, 2)), StandardCharsets.UTF_8);

        assertTrue(csv.contains("Tombamento;Bem;Setor;Categoria;Estado de conservação;Nº de OS;Custo de manutenção;Motivo"));
        assertTrue(csv.contains("T-1;Bem T-1;Sala 1;Info;Ruim;0;0,00;Estado ruim/inservível"));
    }

    @Test
    void pdfDeCriticos_geraPdfValido() {
        when(assetRepository.findByConservationStates(any(), any())).thenReturn(List.of(
                asset(1, "T-1", "Info", "Sala 1", null, null, AssetConservationState.BAD)));
        when(serviceOrderRepository.findForReport(any(), any(), any(), any(), any(), any(), any())).thenReturn(List.of());

        byte[] pdf = service.criticalAssetsPdf(new Critical(null, null, null, 2));

        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
    }
}
