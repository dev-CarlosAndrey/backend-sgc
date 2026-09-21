package br.com.fpo.sgc.domain.report;

import br.com.fpo.sgc.domain.asset.Asset;
import br.com.fpo.sgc.domain.asset.movement.AssetMovement;
import br.com.fpo.sgc.domain.asset.movement.AssetMovementRepository;
import br.com.fpo.sgc.domain.audit.AuditLog;
import br.com.fpo.sgc.domain.audit.AuditLogRepository;
import br.com.fpo.sgc.domain.location.sector.Sector;
import br.com.fpo.sgc.domain.report.ReportFilters.Audit;
import br.com.fpo.sgc.domain.report.ReportFilters.Movements;
import br.com.fpo.sgc.domain.report.dto.AuditReportDTO;
import br.com.fpo.sgc.domain.report.dto.MovementReportDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class TraceabilityReportServiceImplTest {

    private AssetMovementRepository movementRepository;
    private AuditLogRepository auditLogRepository;
    private TraceabilityReportServiceImpl service;

    @BeforeEach
    void setUp() {
        movementRepository = mock(AssetMovementRepository.class);
        auditLogRepository = mock(AuditLogRepository.class);
        service = new TraceabilityReportServiceImpl(movementRepository, auditLogRepository);
    }

    private static AssetMovement movement(long assetId, String tag, String origin, String destination, String reason) {
        return AssetMovement.builder()
                .asset(Asset.builder().id(assetId).tagNumber(tag).name("Bem " + tag).build())
                .originSector(Sector.builder().name(origin).build())
                .destinationSector(Sector.builder().name(destination).build())
                .reason(reason).movementDate(Instant.parse("2026-09-10T15:30:00Z")).build();
    }

    @Test
    void movimentacoes_resumePorBemEPorSetor() {
        when(movementRepository.findForReport(any(), any(), any(), any())).thenReturn(List.of(
                movement(1, "T-1", "Sala 1", "Sala 2", "Troca"),
                movement(1, "T-1", "Sala 2", "Sala 3", null),
                movement(2, "T-2", "Sala 1", "Sala 3", "Reforma")));

        MovementReportDTO report = service.movements(new Movements(null, null, null, null));

        assertEquals(3, report.total());
        assertEquals(2, report.distinctAssets());
        assertEquals("T-1", report.topAssets().get(0).tagNumber());
        assertEquals(2, report.topAssets().get(0).total());
        assertEquals("Sala 1", report.byOriginSector().get(0).key());
        assertEquals(2, report.byOriginSector().get(0).total());
        assertEquals("Sala 3", report.byDestinationSector().get(0).key());
        assertEquals(2, report.byDestinationSector().get(0).total());
    }

    @Test
    void csvDeMovimentacoes_temDataLocalEMotivoVazioQuandoNulo() {
        when(movementRepository.findForReport(any(), any(), any(), any())).thenReturn(List.of(
                movement(1, "T-1", "Sala 1", "Sala 2", null)));

        String csv = new String(service.movementsCsv(new Movements(null, null, null, null)), StandardCharsets.UTF_8);

        assertTrue(csv.contains("Data;Tombamento;Bem;Setor de origem;Setor de destino;Motivo"));
        assertTrue(csv.contains("10/09/2026 12:30;T-1;Bem T-1;Sala 1;Sala 2;"));
    }

    @Test
    void auditoria_somaTotalEUsaRotuloParaUsuarioNulo() {
        when(auditLogRepository.countByEntity(any(), any(), any(), any())).thenReturn(List.<Object[]>of(
                new Object[]{"Asset", 5L}, new Object[]{"ServiceOrder", 2L}));
        when(auditLogRepository.countByAction(any(), any(), any(), any())).thenReturn(List.<Object[]>of(
                new Object[]{"CREATE", 6L}, new Object[]{"UPDATE", 1L}));
        when(auditLogRepository.countByUser(any(), any(), any(), any())).thenReturn(List.<Object[]>of(
                new Object[]{"admin", 6L}, new Object[]{null, 1L}));

        AuditReportDTO report = service.audit(new Audit(null, null, null, null));

        assertEquals(7, report.total());
        assertEquals("Asset", report.byEntity().get(0).key());
        assertEquals("CREATE", report.byAction().get(0).key());
        assertEquals("Sem usuário", report.byUser().get(1).key());
    }

    @Test
    void auditoria_filtrosEmBrancoViramNulosELimitaOCsv() {
        AuditLog event = AuditLog.builder().entityName("Asset").entityId(7L).action("CREATE").performedBy("admin")
                .details("Criado").createdAt(Instant.parse("2026-09-10T15:30:00Z")).build();
        when(auditLogRepository.findForReport(any(), any(), any(), any(), any())).thenReturn(List.of(event));

        String csv = new String(service.auditCsv(new Audit(null, null, " ", "")), StandardCharsets.UTF_8);

        verify(auditLogRepository).findForReport(eq(null), eq(null), eq(null), eq(null), any());
        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        verify(auditLogRepository).findForReport(any(), any(), any(), any(), page.capture());
        assertEquals(TraceabilityReportServiceImpl.AUDIT_CSV_LIMIT, page.getValue().getPageSize());
        assertTrue(csv.contains("Data;Entidade;ID;Ação;Usuário;Detalhes"));
        assertTrue(csv.contains("10/09/2026 12:30;Asset;7;CREATE;admin;Criado"));
    }
}
