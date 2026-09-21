package br.com.fpo.sgc.domain.report;

import br.com.fpo.sgc.domain.serviceorder.ServiceOrderPeriodField;
import br.com.fpo.sgc.domain.serviceorder.ServiceOrderStatus;

import java.time.LocalDate;

/** Todos os campos são opcionais; sem nenhum, o relatório cobre todas as ordens ativas. */
public record ServiceOrderReportFilter(
        LocalDate from,
        LocalDate to,
        ServiceOrderPeriodField field,
        Long technicianId,
        ServiceOrderStatus status,
        Long companyId
) {
}
