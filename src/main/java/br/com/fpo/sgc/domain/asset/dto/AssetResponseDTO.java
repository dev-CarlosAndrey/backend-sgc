package br.com.fpo.sgc.domain.asset.dto;

import br.com.fpo.sgc.domain.asset.AssetStatus;
import br.com.fpo.sgc.domain.asset.category.dto.AssetCategoryResponseDTO;
import br.com.fpo.sgc.domain.location.sector.dto.SectorResponseDTO;

import java.time.Instant;

public record AssetResponseDTO(
        Long id,
        String tagNumber,
        String name,
        String description,
        String model,
        String serialNumber,
        AssetStatus status,
        SectorResponseDTO sector,
        AssetCategoryResponseDTO category,
        Boolean active,
        Instant createdAt
) {
}
