package br.com.fpo.sgc.domain.report.dto;

import java.util.List;

/** Resumo do log de auditoria do filtro; os eventos completos saem no CSV. Listas em ordem decrescente. */
public record AuditReportDTO(long total, List<LabelCountDTO> byEntity, List<LabelCountDTO> byAction,
                             List<LabelCountDTO> byUser) {
}
