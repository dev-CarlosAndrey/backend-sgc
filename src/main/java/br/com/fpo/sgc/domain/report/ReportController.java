package br.com.fpo.sgc.domain.report;

import br.com.fpo.sgc.domain.report.dto.AssetReportDTO;
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

    private ResponseEntity<byte[]> csv(String filename, byte[] content) {
        return ResponseEntity.ok()
                .contentType(CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .body(content);
    }
}
