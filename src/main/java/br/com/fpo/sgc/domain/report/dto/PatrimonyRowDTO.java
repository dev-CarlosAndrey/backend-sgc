package br.com.fpo.sgc.domain.report.dto;

import java.math.BigDecimal;

/** groupId nulo = bens sem categoria. */
public record PatrimonyRowDTO(
        Long groupId,
        String groupName,
        long assets,
        long assetsWithoutValuation,
        BigDecimal acquisitionValue,
        BigDecimal accumulatedDepreciation,
        BigDecimal bookValue
) {
}
