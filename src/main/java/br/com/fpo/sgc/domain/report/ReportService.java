package br.com.fpo.sgc.domain.report;

import br.com.fpo.sgc.domain.report.dto.AssetReportDTO;
import br.com.fpo.sgc.domain.report.dto.MaintenanceCostReportDTO;
import br.com.fpo.sgc.domain.report.dto.ServiceOrderReportDTO;

public interface ReportService {
    AssetReportDTO assetsReport(Long companyId);
    byte[] assetsReportCsv(Long companyId);
    byte[] assetsReportPdf(Long companyId);
    ServiceOrderReportDTO serviceOrdersReport(ServiceOrderReportFilter filter);
    byte[] serviceOrdersCsv(ServiceOrderReportFilter filter);
    byte[] serviceOrdersPdf(ServiceOrderReportFilter filter);
    MaintenanceCostReportDTO maintenanceCostReport(MaintenanceCostFilter filter);
    byte[] maintenanceCostCsv(MaintenanceCostFilter filter);
    byte[] maintenanceCostPdf(MaintenanceCostFilter filter);
}
