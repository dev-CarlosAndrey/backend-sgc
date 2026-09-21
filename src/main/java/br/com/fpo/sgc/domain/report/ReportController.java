package br.com.fpo.sgc.domain.report;

import br.com.fpo.sgc.domain.report.ReportFilters.Audit;
import br.com.fpo.sgc.domain.report.ReportFilters.Critical;
import br.com.fpo.sgc.domain.report.ReportFilters.GroupBy;
import br.com.fpo.sgc.domain.report.ReportFilters.Movements;
import br.com.fpo.sgc.domain.report.ReportFilters.Patrimony;
import br.com.fpo.sgc.domain.report.ReportFilters.Period;
import br.com.fpo.sgc.domain.report.dto.AssetReportDTO;
import br.com.fpo.sgc.domain.report.dto.AuditReportDTO;
import br.com.fpo.sgc.domain.report.dto.CriticalAssetsReportDTO;
import br.com.fpo.sgc.domain.report.dto.MonthlyServiceOrdersReportDTO;
import br.com.fpo.sgc.domain.report.dto.MovementReportDTO;
import br.com.fpo.sgc.domain.report.dto.PatrimonyReportDTO;
import br.com.fpo.sgc.domain.report.dto.StageTimeReportDTO;
import br.com.fpo.sgc.domain.report.dto.MaintenanceCostReportDTO;
import br.com.fpo.sgc.domain.report.dto.ServiceOrderReportDTO;
import br.com.fpo.sgc.domain.serviceorder.ServiceOrderPeriodField;
import br.com.fpo.sgc.domain.serviceorder.ServiceOrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private static final MediaType CSV = MediaType.parseMediaType("text/csv;charset=UTF-8");

    private final ReportService reportService;
    private final ServiceOrderInsightsService serviceOrderInsights;
    private final AssetInsightsService assetInsights;
    private final TraceabilityReportService traceability;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/assets")
    public ResponseEntity<AssetReportDTO> assetsReport(@RequestParam(required = false) Long companyId) {
        return ResponseEntity.ok(reportService.assetsReport(companyId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/assets/export")
    public ResponseEntity<byte[]> exportAssetsReport(@RequestParam(required = false) Long companyId) {
        return csv("bens-por-categoria.csv", reportService.assetsReportCsv(companyId));
    }

    // Datas inclusivas (yyyy-MM-dd); field = OPENED (abertura, padrão) ou CLOSED (conclusão/cancelamento).
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/service-orders")
    public ResponseEntity<ServiceOrderReportDTO> serviceOrdersReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "OPENED") ServiceOrderPeriodField field,
            @RequestParam(required = false) Long technicianId,
            @RequestParam(required = false) ServiceOrderStatus status,
            @RequestParam(required = false) Long companyId) {
        return ResponseEntity.ok(reportService.serviceOrdersReport(
                new ServiceOrderReportFilter(from, to, field, technicianId, status, companyId)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/service-orders/export")
    public ResponseEntity<byte[]> exportServiceOrdersReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "OPENED") ServiceOrderPeriodField field,
            @RequestParam(required = false) Long technicianId,
            @RequestParam(required = false) ServiceOrderStatus status,
            @RequestParam(required = false) Long companyId) {
        return csv("ordens-de-servico.csv", reportService.serviceOrdersCsv(
                new ServiceOrderReportFilter(from, to, field, technicianId, status, companyId)));
    }

    // Custo de manutenção (OS RESOLVED com custo) × valor de aquisição do bem. threshold em %: acima dele o bem
    // é sinalizado como candidato a substituição. field = CLOSED (padrão, quando o custo foi realizado) ou OPENED.
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/maintenance-cost")
    public ResponseEntity<MaintenanceCostReportDTO> maintenanceCostReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "CLOSED") ServiceOrderPeriodField field,
            @RequestParam(required = false) Long companyId,
            @RequestParam(defaultValue = "50") double threshold) {
        return ResponseEntity.ok(reportService.maintenanceCostReport(
                new MaintenanceCostFilter(from, to, field, companyId, threshold)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/maintenance-cost/export")
    public ResponseEntity<byte[]> exportMaintenanceCostReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "CLOSED") ServiceOrderPeriodField field,
            @RequestParam(required = false) Long companyId,
            @RequestParam(defaultValue = "50") double threshold) {
        return csv("custo-manutencao-por-bem.csv", reportService.maintenanceCostCsv(
                new MaintenanceCostFilter(from, to, field, companyId, threshold)));
    }

    // ---------- relatórios de análise ----------

    // Evolução mensal de OS: abertas (por abertura) × concluídas (por conclusão). Padrão: últimos 12 meses; máx. 60.
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/service-orders/monthly")
    public ResponseEntity<MonthlyServiceOrdersReportDTO> monthlyServiceOrders(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long companyId) {
        return ResponseEntity.ok(serviceOrderInsights.monthly(new Period(from, to, companyId)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/service-orders/monthly/export")
    public ResponseEntity<byte[]> exportMonthlyServiceOrders(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long companyId) {
        return csv("os-por-mes.csv", serviceOrderInsights.monthlyCsv(new Period(from, to, companyId)));
    }

    // Tempo em cada etapa do fluxo da OS, calculado sobre as OS abertas no período.
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/service-orders/stage-times")
    public ResponseEntity<StageTimeReportDTO> stageTimes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long companyId) {
        return ResponseEntity.ok(serviceOrderInsights.stageTimes(new Period(from, to, companyId)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/service-orders/stage-times/export")
    public ResponseEntity<byte[]> exportStageTimes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long companyId) {
        return csv("tempo-por-etapa.csv", serviceOrderInsights.stageTimesCsv(new Period(from, to, companyId)));
    }

    // Depreciação linear em usefulLifeYears (padrão 10) na data asOf (padrão hoje); groupBy = CATEGORY (padrão) | SECTOR.
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/assets/patrimony")
    public ResponseEntity<PatrimonyReportDTO> patrimony(
            @RequestParam(required = false) Long companyId,
            @RequestParam(defaultValue = "CATEGORY") GroupBy groupBy,
            @RequestParam(defaultValue = "10") int usefulLifeYears,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return ResponseEntity.ok(assetInsights.patrimony(new Patrimony(companyId, groupBy, usefulLifeYears, asOf)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/assets/patrimony/export")
    public ResponseEntity<byte[]> exportPatrimony(
            @RequestParam(required = false) Long companyId,
            @RequestParam(defaultValue = "CATEGORY") GroupBy groupBy,
            @RequestParam(defaultValue = "10") int usefulLifeYears,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return csv("patrimonio.csv", assetInsights.patrimonyCsv(new Patrimony(companyId, groupBy, usefulLifeYears, asOf)));
    }

    // Bens em estado ruim/inservível ou com pelo menos minOrders OS (não canceladas) abertas no período.
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/assets/critical")
    public ResponseEntity<CriticalAssetsReportDTO> criticalAssets(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "2") int minOrders) {
        return ResponseEntity.ok(assetInsights.criticalAssets(new Critical(companyId, from, to, minOrders)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/assets/critical/export")
    public ResponseEntity<byte[]> exportCriticalAssets(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "2") int minOrders) {
        return csv("bens-criticos.csv", assetInsights.criticalAssetsCsv(new Critical(companyId, from, to, minOrders)));
    }

    // Resumo das movimentações no período; a lista completa está no CSV.
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/movements")
    public ResponseEntity<MovementReportDTO> movements(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long assetId,
            @RequestParam(required = false) Long companyId) {
        return ResponseEntity.ok(traceability.movements(new Movements(from, to, assetId, companyId)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/movements/export")
    public ResponseEntity<byte[]> exportMovements(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long assetId,
            @RequestParam(required = false) Long companyId) {
        return csv("movimentacoes.csv", traceability.movementsCsv(new Movements(from, to, assetId, companyId)));
    }

    // Resumo do log de auditoria; o CSV traz os 10.000 eventos mais recentes do filtro.
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/audit")
    public ResponseEntity<AuditReportDTO> audit(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String entityName,
            @RequestParam(required = false) String performedBy) {
        return ResponseEntity.ok(traceability.audit(new Audit(from, to, entityName, performedBy)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/audit/export")
    public ResponseEntity<byte[]> exportAudit(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String entityName,
            @RequestParam(required = false) String performedBy) {
        return csv("auditoria.csv", traceability.auditCsv(new Audit(from, to, entityName, performedBy)));
    }

    private ResponseEntity<byte[]> csv(String filename, byte[] content) {
        return ResponseEntity.ok()
                .contentType(CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .body(content);
    }
}
