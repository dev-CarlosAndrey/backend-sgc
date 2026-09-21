package br.com.fpo.sgc.domain.report.dto;

import java.util.Map;

/** categoryId nulo = bens sem categoria. byStatus traz todas as situações (zeradas inclusive). */
public record CategoryReportRowDTO(Long categoryId, String categoryName, long total, Map<String, Long> byStatus) {
}
