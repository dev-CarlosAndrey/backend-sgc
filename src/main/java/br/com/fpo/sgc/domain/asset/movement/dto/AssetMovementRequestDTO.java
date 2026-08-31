package br.com.fpo.sgc.domain.asset.movement.dto;

import jakarta.validation.constraints.NotNull;

public record AssetMovementRequestDTO(
        @NotNull(message = "O ID do ativo é obrigatório")
        Long assetId,

        @NotNull(message = "O ID do setor de destino é obrigatório")
        Long destinationSectorId,

        String reason
) {}