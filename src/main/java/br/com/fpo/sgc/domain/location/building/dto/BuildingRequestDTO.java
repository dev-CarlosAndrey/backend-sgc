package br.com.fpo.sgc.domain.location.building.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BuildingRequestDTO(
        @NotBlank(message = "O nome do prédio é obrigatório")
        String name,

        @NotBlank(message = "O endereço é obrigatório")
        String address,

        @NotNull(message = "A empresa vinculada é obrigatória")
        Long companyId
) {}