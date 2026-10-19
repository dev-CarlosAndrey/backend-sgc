package br.com.fpo.sgc.domain.report.dto;

/**
 * Um canal de abertura. pending = OPEN + IN_PROGRESS. avgResolutionHours considera só as OS RESOLVED
 * (abertura → conclusão); nulo quando não há nenhuma. sharePercent é sobre o total do período.
 */
public record ChannelRowDTO(String channel, long total, Double sharePercent, long resolved, long cancelled,
                            long pending, Double avgResolutionHours) {
}
