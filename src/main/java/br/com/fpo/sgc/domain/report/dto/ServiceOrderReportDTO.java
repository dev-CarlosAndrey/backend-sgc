package br.com.fpo.sgc.domain.report.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * avgResolutionHours = média (em horas) entre abertura e conclusão das ordens RESOLVED do filtro;
 * nulo quando não há nenhuma.
 */
public record ServiceOrderReportDTO(
        long total,
        long resolved,
        BigDecimal totalCost,
        Double avgResolutionHours,
        List<LabelCountDTO> byStatus,
        List<LabelCountDTO> byPriority,
        List<TechnicianReportRowDTO> byTechnician,
        List<AssetMaintenanceRowDTO> topAssets
) {
}
