package br.com.fpo.sgc.domain.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductRequestDTO(
        @NotBlank(message = "O nome do produto/modelo é obrigatório")
        @Size(min = 2, max = 150, message = "O nome deve ter entre 2 e 150 caracteres")
        String name,

        String description,

        @NotNull(message = "O ID da marca é obrigatório")
        Long brandId,

        @NotNull(message = "O ID do material/categoria é obrigatório")
        Long materialId,

        Boolean active
) {}