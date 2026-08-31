package br.com.fpo.sgc.domain.asset.movement;

import br.com.fpo.sgc.domain.asset.movement.dto.AssetMovementRequestDTO;
import br.com.fpo.sgc.domain.asset.movement.dto.AssetMovementResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AssetMovementService {
    AssetMovementResponseDTO create(AssetMovementRequestDTO dto);
    Page<AssetMovementResponseDTO> findAll(Pageable pageable);
    Page<AssetMovementResponseDTO> findByAssetId(Long assetId, Pageable pageable);
}