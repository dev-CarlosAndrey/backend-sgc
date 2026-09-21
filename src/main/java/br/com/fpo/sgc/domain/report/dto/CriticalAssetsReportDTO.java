package br.com.fpo.sgc.domain.report.dto;

import java.util.List;

public record CriticalAssetsReportDTO(int minOrders, long total, List<CriticalAssetRowDTO> assets) {
}
