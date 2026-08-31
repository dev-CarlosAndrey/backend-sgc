package br.com.fpo.sgc.domain.location.sector.dto;

import br.com.fpo.sgc.domain.location.building.dto.BuildingResponseDTO;
import java.time.Instant;

public record SectorResponseDTO(
        Long id,
        String name,
        BuildingResponseDTO building,
        Long managerId,
        String managerName,
        String managerAvatar,
        long totalAssets,
        long activeTickets,
        Boolean active,
        Instant createdAt
) {}