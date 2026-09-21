package br.com.fpo.sgc.domain.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Patrimônio com depreciação linear (sem valor residual) em usefulLifeYears anos, na data asOf.
 * Bens baixados ficam de fora; bens sem data ou valor de aquisição são contados em assetsWithoutValuation
 * e não entram nos valores.
 */
public record PatrimonyReportDTO(
        LocalDate asOf,
        int usefulLifeYears,
        long assets,
        long assetsWithoutValuation,
        BigDecimal acquisitionValue,
        BigDecimal accumulatedDepreciation,
        BigDecimal bookValue,
        List<PatrimonyRowDTO> groups
) {
}
