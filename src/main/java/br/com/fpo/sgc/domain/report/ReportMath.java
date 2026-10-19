package br.com.fpo.sgc.domain.report;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;

/** Utilitários numéricos dos relatórios de análise. */
final class ReportMath {

    private ReportMath() {
    }

    static Double hours(Duration duration) {
        return round1(duration.toMinutes() / 60.0);
    }

    // Média em horas (1 casa); nulo sem amostras.
    static Double averageHours(List<Duration> durations) {
        if (durations.isEmpty()) {
            return null;
        }
        return round1(durations.stream().mapToLong(Duration::toMinutes).average().orElse(0) / 60.0);
    }

    // Percentual com 1 casa; nulo quando o total é zero (não há base para a taxa).
    static Double percent(long part, long total) {
        return total == 0 ? null : round1(part * 100.0 / total);
    }

    static String money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }

    private static double round1(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
