package br.com.fpo.sgc.domain.report.dto;

/** technicianId nulo = ordens ainda sem técnico atribuído. */
public record TechnicianReportRowDTO(Long technicianId, String technicianName, long total, long resolved,
                                     Double avgResolutionHours) {
}
