package br.com.fpo.sgc.domain.serviceorder.dto;

import br.com.fpo.sgc.domain.asset.dto.AssetResponseDTO;
import br.com.fpo.sgc.domain.location.sector.dto.SectorResponseDTO;
import br.com.fpo.sgc.domain.serviceorder.ServiceOrderPriority;
import br.com.fpo.sgc.domain.serviceorder.ServiceOrderStatus;
import br.com.fpo.sgc.domain.user.dto.UserResponseDTO;

import java.time.Instant;

public record ServiceOrderResponseDTO(
        Long id,
        String title,
        String description,
        ServiceOrderStatus status,
        ServiceOrderPriority priority,
        UserResponseDTO requester,
        UserResponseDTO technician,
        AssetResponseDTO asset,
        SectorResponseDTO sector,
        String solutionDetails,
        Instant closedAt,
        Boolean active,
        Instant createdAt
) {
}