package br.com.fpo.sgc.domain.report.dto;

import java.math.BigDecimal;

/**
 * costPercentOfValue = custo acumulado de manutenção / valor de aquisição, em %; nulo quando o bem
 * não tem valor de aquisição informado (ou é zero). exceedsThreshold indica bem candidato a substituição.
 */
public record AssetCostRowDTO(
        Long assetId,
        String tagNumber,
        String name,
        String categoryName,
        BigDecimal acquisitionValue,
        long orders,
        BigDecimal maintenanceCost,
        Double costPercentOfValue,
        boolean exceedsThreshold
) {
}
