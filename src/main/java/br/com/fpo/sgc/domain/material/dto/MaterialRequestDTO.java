package br.com.fpo.sgc.domain.material.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MaterialRequestDTO(
        @NotBlank(message = "O nome do material é obrigatório")
        @Size(min = 2, max = 100, message = "O nome deve ter entre 2 e 100 caracteres")
        String name,

        @NotBlank(message = "A categoria do material é obrigatória")
        @Size(min = 2, max = 100, message = "A categoria deve ter entre 2 e 100 caracteres")
        String category,

        Boolean active
) {}