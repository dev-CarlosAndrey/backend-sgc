package br.com.fpo.sgc.domain.report.dto;

/** samples = quantas passagens por essa etapa já terminaram; médias/máximo em horas (nulos sem amostras). */
public record StageTimeRowDTO(String status, long samples, Double avgHours, Double maxHours) {
}
