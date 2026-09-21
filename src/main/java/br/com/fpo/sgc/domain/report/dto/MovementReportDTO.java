package br.com.fpo.sgc.domain.report.dto;

import java.util.List;

/** Resumo das movimentações do filtro; a lista completa sai no CSV. key dos setores = nome do setor. */
public record MovementReportDTO(long total, long distinctAssets, List<MovementAssetRowDTO> topAssets,
                                List<LabelCountDTO> byOriginSector, List<LabelCountDTO> byDestinationSector) {
}
