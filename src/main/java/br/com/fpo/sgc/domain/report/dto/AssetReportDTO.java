package br.com.fpo.sgc.domain.report.dto;

import java.util.List;

public record AssetReportDTO(long total, List<LabelCountDTO> byStatus, List<CategoryReportRowDTO> byCategory) {
}
