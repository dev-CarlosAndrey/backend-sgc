package br.com.fpo.sgc.domain.asset.movement.mapper;

import br.com.fpo.sgc.domain.asset.Asset;
import br.com.fpo.sgc.domain.asset.movement.AssetMovement;
import br.com.fpo.sgc.domain.asset.movement.dto.AssetMovementRequestDTO;
import br.com.fpo.sgc.domain.asset.movement.dto.AssetMovementResponseDTO;
import br.com.fpo.sgc.domain.location.sector.Sector;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class AssetMovementMapper {

    public AssetMovement toEntity(AssetMovementRequestDTO dto, Asset asset, Sector originSector, Sector destinationSector) {
        if (dto == null) return null;

        return AssetMovement.builder()
                .asset(asset)
                .originSector(originSector)
                .destinationSector(destinationSector)
                .reason(dto.reason())
                .movementDate(Instant.now())
                .build();
    }

    public AssetMovementResponseDTO toDTO(AssetMovement movement) {
        if (movement == null) return null;

        return new AssetMovementResponseDTO(
                movement.getId(),
                movement.getAsset() != null ? movement.getAsset().getId() : null,
                movement.getAsset() != null ? movement.getAsset().getTagNumber() : null,
                movement.getAsset() != null ? movement.getAsset().getName() : null,
                movement.getOriginSector() != null ? movement.getOriginSector().getId() : null,
                movement.getOriginSector() != null ? movement.getOriginSector().getName() : null,
                movement.getDestinationSector() != null ? movement.getDestinationSector().getId() : null,
                movement.getDestinationSector() != null ? movement.getDestinationSector().getName() : null,
                movement.getReason(),
                movement.getMovementDate()
        );
    }
}