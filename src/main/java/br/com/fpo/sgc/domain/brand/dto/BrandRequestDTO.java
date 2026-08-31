package br.com.fpo.sgc.domain.brand.dto;

import jakarta.validation.constraints.NotBlank;

public record BrandRequestDTO(
        @NotBlank(message = "O nome da marca é obrigatório")
        String name
) {}