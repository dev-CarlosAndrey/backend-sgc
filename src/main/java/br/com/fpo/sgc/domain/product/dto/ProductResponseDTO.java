package br.com.fpo.sgc.domain.product.dto;

import java.time.Instant;

public record ProductResponseDTO(
        Long id,
        String name,
        String description,
        Long brandId,
        String brandName,
        Long materialId,
        String materialName,
        String category,
        Boolean active,
        Instant createdAt
) {}