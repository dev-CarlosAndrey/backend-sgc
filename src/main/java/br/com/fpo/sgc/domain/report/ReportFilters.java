package br.com.fpo.sgc.domain.report;

import java.time.LocalDate;

/** Filtros dos relatórios de análise. Todos os campos são opcionais, salvo onde indicado. */
public final class ReportFilters {

    private ReportFilters() {
    }

    public record Period(LocalDate from, LocalDate to, Long companyId) {
    }

    public record Patrimony(Long companyId, GroupBy groupBy, int usefulLifeYears, LocalDate asOf) {
    }

    public record Critical(Long companyId, LocalDate from, LocalDate to, int minOrders) {
    }

    public record Movements(LocalDate from, LocalDate to, Long assetId, Long companyId) {
    }

    public record Audit(LocalDate from, LocalDate to, String entityName, String performedBy) {
    }

    public enum GroupBy {
        CATEGORY,
        SECTOR
    }
}
