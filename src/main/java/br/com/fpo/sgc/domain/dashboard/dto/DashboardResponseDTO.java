package br.com.fpo.sgc.domain.dashboard.dto;

import java.util.Map;

public record DashboardResponseDTO(
        long totalAssets,
        long activeTickets,
        double maintenanceCost,
        long depreciatingAssets,
        Map<String, Long> assetsByStatus,
        Map<String, Long> assetsByCompany
) {}