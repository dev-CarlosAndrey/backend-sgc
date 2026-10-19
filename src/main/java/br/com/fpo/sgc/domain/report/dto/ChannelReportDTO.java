package br.com.fpo.sgc.domain.report.dto;

import java.util.List;

/** OS por canal de abertura (painel × WhatsApp) no período, com a evolução mês a mês. */
public record ChannelReportDTO(long total, List<ChannelRowDTO> channels, List<ChannelMonthRowDTO> months) {
}
