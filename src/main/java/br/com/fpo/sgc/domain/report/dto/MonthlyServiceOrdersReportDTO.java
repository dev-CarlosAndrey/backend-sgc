package br.com.fpo.sgc.domain.report.dto;

import java.util.List;

/** Um item por mês do período (zerado quando não houve movimento), do mais antigo para o mais recente. */
public record MonthlyServiceOrdersReportDTO(long totalOpened, long totalResolved, List<MonthlyServiceOrderRowDTO> months) {
}
