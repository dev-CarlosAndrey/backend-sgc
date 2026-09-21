package br.com.fpo.sgc.domain.report;

import br.com.fpo.sgc.domain.report.ReportFilters.Audit;
import br.com.fpo.sgc.domain.report.ReportFilters.Movements;
import br.com.fpo.sgc.domain.report.dto.AuditReportDTO;
import br.com.fpo.sgc.domain.report.dto.MovementReportDTO;

/** Relatórios de rastreabilidade: movimentações de bens e log de auditoria. */
public interface TraceabilityReportService {
    MovementReportDTO movements(Movements filter);
    byte[] movementsCsv(Movements filter);
    AuditReportDTO audit(Audit filter);
    byte[] auditCsv(Audit filter);
}
