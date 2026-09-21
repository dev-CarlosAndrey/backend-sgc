package br.com.fpo.sgc.domain.report;

import br.com.fpo.sgc.domain.serviceorder.ServiceOrderPeriodField;

import java.time.LocalDate;

/** Todos opcionais; threshold em % (padrão aplicado pelo controller). */
public record MaintenanceCostFilter(
        LocalDate from,
        LocalDate to,
        ServiceOrderPeriodField field,
        Long companyId,
        double thresholdPercent
) {
}
