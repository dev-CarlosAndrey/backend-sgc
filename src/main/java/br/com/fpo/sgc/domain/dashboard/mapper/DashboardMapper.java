package br.com.fpo.sgc.domain.dashboard.mapper;

import br.com.fpo.sgc.domain.dashboard.dto.DashboardResponseDTO;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DashboardMapper {

    public DashboardResponseDTO toDTO(
            long totalAssets,
            long activeTickets,
            double maintenanceCost,
            long depreciatingAssets,
            Map<String, Long> assetsByStatus,
            Map<String, Long> assetsByCompany
    ) {
        return new DashboardResponseDTO(
                totalAssets,
                activeTickets,
                maintenanceCost,
                depreciatingAssets,
                assetsByStatus,
                assetsByCompany
        );
    }
}