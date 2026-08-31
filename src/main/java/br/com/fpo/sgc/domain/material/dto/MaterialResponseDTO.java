package br.com.fpo.sgc.domain.material.dto;

import java.time.Instant;

public record MaterialResponseDTO(
        Long id,
        String name,
        String category,
        Boolean active,
        Instant createdAt
) {}