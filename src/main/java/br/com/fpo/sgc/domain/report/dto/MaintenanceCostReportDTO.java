package br.com.fpo.sgc.domain.report.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Custo de manutenção × valor do bem. Só entram bens com ao menos uma OS RESOLVED com custo no filtro.
 * totalAcquisitionValue soma o valor de aquisição apenas dos bens listados.
 */
public record MaintenanceCostReportDTO(
        double thresholdPercent,
        BigDecimal totalMaintenanceCost,
        BigDecimal totalAcquisitionValue,
        long assetsAboveThreshold,
        List<AssetCostRowDTO> assets
) {
}
