package br.com.fpo.sgc.domain.asset.movement;

import br.com.fpo.sgc.common.exception.BusinessException;
import br.com.fpo.sgc.common.util.SearchTerm;
import br.com.fpo.sgc.domain.asset.Asset;
import br.com.fpo.sgc.domain.asset.AssetRepository;
import br.com.fpo.sgc.domain.asset.movement.dto.AssetMovementRequestDTO;
import br.com.fpo.sgc.domain.asset.movement.dto.AssetMovementResponseDTO;
import br.com.fpo.sgc.domain.asset.movement.mapper.AssetMovementMapper;
import br.com.fpo.sgc.domain.audit.AuditLogService;
import br.com.fpo.sgc.domain.location.sector.Sector;
import br.com.fpo.sgc.domain.location.sector.SectorRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AssetMovementServiceImpl implements AssetMovementService {

    private final AssetMovementRepository movementRepository;
    private final AssetRepository assetRepository;
    private final SectorRepository sectorRepository;
    private final AssetMovementMapper movementMapper;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public AssetMovementResponseDTO create(AssetMovementRequestDTO dto) {
        Asset asset = assetRepository.findById(dto.assetId())
                .orElseThrow(() -> new EntityNotFoundException("Ativo não encontrado com ID: " + dto.assetId()));

        Sector destinationSector = sectorRepository.findById(dto.destinationSectorId())
                .orElseThrow(() -> new EntityNotFoundException("Setor de destino não encontrado com ID: " + dto.destinationSectorId()));

        Sector originSector = asset.getSector();

        if (originSector.getId().equals(destinationSector.getId())) {
            throw new BusinessException("O setor de destino deve ser diferente do setor de origem atual.");
        }

        // 1. Cria a movimentação
        AssetMovement movement = movementMapper.toEntity(dto, asset, originSector, destinationSector);
        AssetMovement savedMovement = movementRepository.save(movement);

        // 2. Atualiza a localização atual do ativo
        asset.setSector(destinationSector);
        assetRepository.save(asset);

        // 3. Registrar auditoria
        auditLogService.log(
                "AssetMovement",
                savedMovement.getId(),
                "TRANSFER",
                "ADMIN",
                "Ativo " + asset.getTagNumber() + " transferido de " + originSector.getName() + " para " + destinationSector.getName()
        );

        return movementMapper.toDTO(savedMovement);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssetMovementResponseDTO> findAll(String q, Long destinationSectorId, Pageable pageable) {
        return movementRepository.search(SearchTerm.toLikePattern(q), destinationSectorId, pageable)
                .map(movementMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssetMovementResponseDTO> findByAssetId(Long assetId, Pageable pageable) {
        return movementRepository.findByAssetIdAndActiveTrueOrderByMovementDateDesc(assetId, pageable)
                .map(movementMapper::toDTO);
    }
}