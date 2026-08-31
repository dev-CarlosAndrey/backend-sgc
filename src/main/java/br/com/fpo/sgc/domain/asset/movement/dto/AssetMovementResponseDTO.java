package br.com.fpo.sgc.domain.asset.movement.dto;

import java.time.Instant;

public record AssetMovementResponseDTO(
        Long id,
        Long assetId,
        String assetTagNumber,
        String assetName,
        Long originSectorId,
        String originSectorName,
        Long destinationSectorId,
        String destinationSectorName,
        String reason,
        Instant movementDate
) {}