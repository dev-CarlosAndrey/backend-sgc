package br.com.fpo.sgc.domain.report.dto;

/** month = yyyy-MM (fuso America/Fortaleza), só meses com pelo menos uma OS aberta. */
public record ChannelMonthRowDTO(String month, long panel, long whatsapp, Double whatsappPercent) {
}
