package br.com.fpo.sgc.domain.location.sector.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SectorRequestDTO(
        @NotBlank(message = "O nome do setor é obrigatório")
        @Size(min = 2, max = 100, message = "O nome deve ter entre 2 e 100 caracteres")
        String name,

        @NotNull(message = "O ID do prédio é obrigatório")
        Long buildingId,

        Boolean active,

        Long managerId
) {}