package br.com.fpo.sgc.domain.report.dto;

/** Tentativas por canal. successRatePercent = sent / attempts (nulo sem tentativas). */
public record NotificationChannelRowDTO(String channel, long attempts, long sent, long failed,
                                        Double successRatePercent) {
}
