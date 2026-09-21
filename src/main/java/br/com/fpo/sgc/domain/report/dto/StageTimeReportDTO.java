package br.com.fpo.sgc.domain.report.dto;

import java.util.List;

/**
 * Tempo de permanência em cada etapa do fluxo da OS (a partir do histórico de status).
 * bottleneck = etapa com maior tempo médio; nulo se não houver amostras.
 */
public record StageTimeReportDTO(long orders, String bottleneck, List<StageTimeRowDTO> stages,
                                 List<PriorityTimeRowDTO> byPriority) {
}
