package br.com.fpo.sgc.domain.report.dto;

/** avgFirstResponseHours = tempo em OPEN até a primeira mudança de status; avgResolutionHours só para OS RESOLVED. */
public record PriorityTimeRowDTO(String priority, long orders, Double avgFirstResponseHours, Double avgResolutionHours) {
}
