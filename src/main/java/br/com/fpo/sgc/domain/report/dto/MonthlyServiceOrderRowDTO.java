package br.com.fpo.sgc.domain.report.dto;

import java.math.BigDecimal;

/** month = yyyy-MM. opened conta por abertura; resolved e cost contam por conclusão (OS RESOLVED). */
public record MonthlyServiceOrderRowDTO(String month, long opened, long resolved, BigDecimal cost) {
}
