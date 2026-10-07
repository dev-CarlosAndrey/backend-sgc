package br.com.fpo.sgc.domain.report;

import br.com.fpo.sgc.common.util.CsvWriter;
import br.com.fpo.sgc.common.util.PdfWriter;
import br.com.fpo.sgc.common.util.PeriodRange;
import br.com.fpo.sgc.common.util.TableData;
import br.com.fpo.sgc.domain.asset.movement.AssetMovement;
import br.com.fpo.sgc.domain.asset.movement.AssetMovementRepository;
import br.com.fpo.sgc.domain.audit.AuditLog;
import br.com.fpo.sgc.domain.audit.AuditLogRepository;
import br.com.fpo.sgc.domain.report.ReportFilters.Audit;
import br.com.fpo.sgc.domain.report.ReportFilters.Movements;
import br.com.fpo.sgc.domain.report.dto.AuditReportDTO;
import br.com.fpo.sgc.domain.report.dto.LabelCountDTO;
import br.com.fpo.sgc.domain.report.dto.MovementAssetRowDTO;
import br.com.fpo.sgc.domain.report.dto.MovementReportDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class TraceabilityReportServiceImpl implements TraceabilityReportService {

    private static final int TOP_ASSETS_LIMIT = 5;
    // O log de auditoria cresce sem parar: o CSV traz os eventos mais recentes do filtro, até este limite.
    static final int AUDIT_CSV_LIMIT = 10_000;
    // PDF é pra leitura/impressão, não dump de dados: limite bem menor que o CSV.
    static final int AUDIT_PDF_LIMIT = 300;
    private static final String NO_USER = "Sem usuário";
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(PeriodRange.BUSINESS_ZONE);

    private final AssetMovementRepository movementRepository;
    private final AuditLogRepository auditLogRepository;

    // ---------- movimentações ----------

    @Override
    @Transactional(readOnly = true)
    public MovementReportDTO movements(Movements filter) {
        List<AssetMovement> movements = fetchMovements(filter);

        Map<Long, MovementAssetRowDTO> assets = new HashMap<>();
        for (AssetMovement m : movements) {
            assets.merge(m.getAsset().getId(),
                    new MovementAssetRowDTO(m.getAsset().getId(), m.getAsset().getTagNumber(), m.getAsset().getName(), 1),
                    (a, b) -> new MovementAssetRowDTO(a.assetId(), a.tagNumber(), a.name(), a.total() + 1));
        }
        List<MovementAssetRowDTO> top = assets.values().stream()
                .sorted(Comparator.comparingLong(MovementAssetRowDTO::total).reversed()
                        .thenComparing(MovementAssetRowDTO::tagNumber))
                .limit(TOP_ASSETS_LIMIT)
                .toList();

        return new MovementReportDTO(movements.size(), assets.size(), top,
                countBy(movements, m -> m.getOriginSector().getName()),
                countBy(movements, m -> m.getDestinationSector().getName()));
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] movementsCsv(Movements filter) {
        TableData table = movementsTable(filter);
        return CsvWriter.toBytes(table.header(), table.rows());
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] movementsPdf(Movements filter) {
        TableData table = movementsTable(filter);
        return PdfWriter.toBytes("Relatório de Movimentações", table.header(), table.rows(), true);
    }

    private TableData movementsTable(Movements filter) {
        List<List<String>> rows = fetchMovements(filter).stream().map(m -> List.of(
                DATE_TIME.format(m.getMovementDate()),
                m.getAsset().getTagNumber(),
                m.getAsset().getName(),
                m.getOriginSector().getName(),
                m.getDestinationSector().getName(),
                m.getReason() != null ? m.getReason() : ""
        )).toList();
        return new TableData(List.of("Data", "Tombamento", "Bem", "Setor de origem", "Setor de destino", "Motivo"), rows);
    }

    private List<AssetMovement> fetchMovements(Movements filter) {
        PeriodRange range = PeriodRange.of(filter.from(), filter.to());
        return movementRepository.findForReport(range.from(), range.to(), filter.assetId(), filter.companyId());
    }

    // Contagem por rótulo, maior primeiro; empate por nome.
    private static List<LabelCountDTO> countBy(List<AssetMovement> movements, Function<AssetMovement, String> label) {
        Map<String, Long> counts = new HashMap<>();
        movements.forEach(m -> counts.merge(label.apply(m), 1L, Long::sum));
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .map(e -> new LabelCountDTO(e.getKey(), e.getValue()))
                .toList();
    }

    // ---------- auditoria ----------

    @Override
    @Transactional(readOnly = true)
    public AuditReportDTO audit(Audit filter) {
        PeriodRange range = PeriodRange.of(filter.from(), filter.to());
        List<LabelCountDTO> byEntity = labels(auditLogRepository.countByEntity(
                range.from(), range.to(), blankToNull(filter.entityName()), blankToNull(filter.performedBy())));
        List<LabelCountDTO> byAction = labels(auditLogRepository.countByAction(
                range.from(), range.to(), blankToNull(filter.entityName()), blankToNull(filter.performedBy())));
        List<LabelCountDTO> byUser = labels(auditLogRepository.countByUser(
                range.from(), range.to(), blankToNull(filter.entityName()), blankToNull(filter.performedBy())));

        return new AuditReportDTO(byEntity.stream().mapToLong(LabelCountDTO::total).sum(), byEntity, byAction, byUser);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] auditCsv(Audit filter) {
        TableData table = auditTable(filter, AUDIT_CSV_LIMIT);
        return CsvWriter.toBytes(table.header(), table.rows());
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] auditPdf(Audit filter) {
        TableData table = auditTable(filter, AUDIT_PDF_LIMIT);
        return PdfWriter.toBytes("Relatório de Auditoria (últimos " + AUDIT_PDF_LIMIT + " eventos do filtro)",
                table.header(), table.rows(), true);
    }

    private TableData auditTable(Audit filter, int limit) {
        PeriodRange range = PeriodRange.of(filter.from(), filter.to());
        List<AuditLog> events = auditLogRepository.findForReport(range.from(), range.to(),
                blankToNull(filter.entityName()), blankToNull(filter.performedBy()), PageRequest.of(0, limit));

        List<List<String>> rows = events.stream().map(e -> List.of(
                DATE_TIME.format(e.getCreatedAt()),
                e.getEntityName(),
                String.valueOf(e.getEntityId()),
                e.getAction(),
                e.getPerformedBy() != null ? e.getPerformedBy() : "",
                e.getDetails() != null ? e.getDetails() : ""
        )).toList();
        return new TableData(List.of("Data", "Entidade", "ID", "Ação", "Usuário", "Detalhes"), rows);
    }

    private static List<LabelCountDTO> labels(List<Object[]> rows) {
        return rows.stream()
                .map(r -> new LabelCountDTO(r[0] != null ? (String) r[0] : NO_USER, (Long) r[1]))
                .toList();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
