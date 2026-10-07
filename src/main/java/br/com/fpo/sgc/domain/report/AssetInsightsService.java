package br.com.fpo.sgc.domain.report;

import br.com.fpo.sgc.domain.report.ReportFilters.Critical;
import br.com.fpo.sgc.domain.report.ReportFilters.Patrimony;
import br.com.fpo.sgc.domain.report.dto.CriticalAssetsReportDTO;
import br.com.fpo.sgc.domain.report.dto.PatrimonyReportDTO;

/** Relatórios de análise sobre os bens: patrimônio com depreciação e bens críticos. */
public interface AssetInsightsService {
    PatrimonyReportDTO patrimony(Patrimony filter);
    byte[] patrimonyCsv(Patrimony filter);
    byte[] patrimonyPdf(Patrimony filter);
    CriticalAssetsReportDTO criticalAssets(Critical filter);
    byte[] criticalAssetsCsv(Critical filter);
    byte[] criticalAssetsPdf(Critical filter);
}
