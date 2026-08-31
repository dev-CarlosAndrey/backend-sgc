package br.com.fpo.sgc.domain.serviceorder.dto;

import br.com.fpo.sgc.domain.serviceorder.ServiceOrderPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ServiceOrderRequestDTO(
        @NotBlank(message = "O título é obrigatório")
        @Size(min = 5, max = 150, message = "O título deve ter entre 5 e 150 caracteres")
        String title,

        @NotBlank(message = "A descrição do problema é obrigatória")
        String description,

        @NotNull(message = "A prioridade é obrigatória")
        ServiceOrderPriority priority,

        @NotNull(message = "O ID do solicitante é obrigatório")
        Long requesterId,

        Long assetId,

        @NotNull(message = "O ID do setor é obrigatório")
        Long sectorId
) {}