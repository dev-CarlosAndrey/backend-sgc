package br.com.fpo.sgc.domain.report.dto;

import java.math.BigDecimal;
import java.util.List;

/** reasons: BAD_CONSERVATION (estado RUIM/INSERVÍVEL) e/ou RECURRENT_MAINTENANCE (OS acima do mínimo). */
public record CriticalAssetRowDTO(
        Long assetId,
        String tagNumber,
        String name,
        String sectorName,
        String categoryName,
        String conservationState,
        long orders,
        BigDecimal maintenanceCost,
        List<String> reasons
) {
}
