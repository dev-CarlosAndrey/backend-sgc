package br.com.fpo.sgc.common.util;

import br.com.fpo.sgc.common.exception.BusinessException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Intervalo [from, to) em instantes, montado a partir de datas inclusivas no fuso da instituição.
 * Qualquer ponta pode ser nula (sem limite naquele lado).
 */
public record PeriodRange(Instant from, Instant to) {

    // Fuso da instituição (Crateús/CE), usado para interpretar as datas informadas pelo usuário.
    public static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Fortaleza");

    public static PeriodRange of(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException("A data inicial não pode ser posterior à data final.");
        }
        Instant fromInstant = from != null ? from.atStartOfDay(BUSINESS_ZONE).toInstant() : null;
        Instant toInstant = to != null ? to.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant() : null;
        return new PeriodRange(fromInstant, toInstant);
    }
}
