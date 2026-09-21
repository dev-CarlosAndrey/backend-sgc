package br.com.fpo.sgc.domain.asset.category.dto;

import java.time.Instant;

public record AssetCategoryResponseDTO(
        Long id,
        String name,
        String description,
        Boolean active,
        Instant createdAt
) {}
