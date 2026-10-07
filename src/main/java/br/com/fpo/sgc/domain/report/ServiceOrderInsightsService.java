package br.com.fpo.sgc.domain.report;

import br.com.fpo.sgc.domain.report.ReportFilters.Period;
import br.com.fpo.sgc.domain.report.dto.MonthlyServiceOrdersReportDTO;
import br.com.fpo.sgc.domain.report.dto.StageTimeReportDTO;

/** Relatórios de análise sobre as ordens de serviço: evolução mensal e tempo por etapa. */
public interface ServiceOrderInsightsService {
    MonthlyServiceOrdersReportDTO monthly(Period filter);
    byte[] monthlyCsv(Period filter);
    byte[] monthlyPdf(Period filter);
    StageTimeReportDTO stageTimes(Period filter);
    byte[] stageTimesCsv(Period filter);
    byte[] stageTimesPdf(Period filter);
}
