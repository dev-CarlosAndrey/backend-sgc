package br.com.fpo.sgc.domain.asset;

import br.com.fpo.sgc.common.exception.BusinessException;
import br.com.fpo.sgc.domain.asset.dto.AssetRequestDTO;
import br.com.fpo.sgc.domain.asset.dto.AssetResponseDTO;
import br.com.fpo.sgc.domain.asset.mapper.AssetMapper;
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
public class AssetServiceImpl implements AssetService {

    private final AssetRepository assetRepository;
    private final SectorRepository sectorRepository;
    private final AuditLogService auditLogService;
    private final AssetMapper assetMapper;

    @Override
    @Transactional
    public AssetResponseDTO create(AssetRequestDTO dto) {
        if (assetRepository.existsByTagNumber(dto.tagNumber())) {
            throw new BusinessException("Já existe um ativo cadastrado com o tombamento: " + dto.tagNumber());
        }

        Sector sector = sectorRepository.findById(dto.sectorId())
                .orElseThrow(() -> new EntityNotFoundException("Setor não encontrado com ID: " + dto.sectorId()));

        Asset asset = assetMapper.toEntity(dto, sector);
        Asset savedAsset = assetRepository.save(asset);

        // Registrar auditoria
        auditLogService.log(
                "Asset",
                savedAsset.getId(),
                "CREATE",
                "ADMIN",
                "Ativo criado com tombamento " + savedAsset.getTagNumber() + " no setor " + sector.getName()
        );

        return assetMapper.toDTO(savedAsset);
    }

    @Override
    @Transactional(readOnly = true)
    public AssetResponseDTO findById(Long id) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ativo não encontrado com ID: " + id));
        return assetMapper.toDTO(asset);
    }

    @Override
    @Transactional(readOnly = true)
    public AssetResponseDTO findByTagNumber(String tagNumber) {
        Asset asset = assetRepository.findByTagNumber(tagNumber)
                .orElseThrow(() -> new EntityNotFoundException("Ativo não encontrado com tombamento: " + tagNumber));
        return assetMapper.toDTO(asset);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssetResponseDTO> findAll(Pageable pageable) {
        return assetRepository.findByActiveTrue(pageable).map(assetMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssetResponseDTO> findBySectorId(Long sectorId, Pageable pageable) {
        return assetRepository.findBySectorIdAndActiveTrue(sectorId, pageable).map(assetMapper::toDTO);
    }

    @Override
    @Transactional
    public AssetResponseDTO update(Long id, AssetRequestDTO dto) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ativo não encontrado com ID: " + id));

        if (!asset.getTagNumber().equalsIgnoreCase(dto.tagNumber()) && assetRepository.existsByTagNumber(dto.tagNumber())) {
            throw new BusinessException("O tombamento informado já está em uso por outro ativo.");
        }

        Sector sector = sectorRepository.findById(dto.sectorId())
                .orElseThrow(() -> new EntityNotFoundException("Setor não encontrado com ID: " + dto.sectorId()));

        asset.setTagNumber(dto.tagNumber());
        asset.setName(dto.name());
        asset.setDescription(dto.description());
        asset.setModel(dto.model());
        asset.setSerialNumber(dto.serialNumber());
        asset.setStatus(dto.status());
        asset.setSector(sector);

        Asset updatedAsset = assetRepository.save(asset);

        // Registrar auditoria
        auditLogService.log(
                "Asset",
                updatedAsset.getId(),
                "UPDATE",
                "ADMIN",
                "Ativo " + updatedAsset.getTagNumber() + " atualizado"
        );

        return assetMapper.toDTO(updatedAsset);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ativo não encontrado com ID: " + id));

        asset.setActive(false);
        assetRepository.save(asset);

        // Registrar auditoria
        auditLogService.log(
                "Asset",
                id,
                "DELETE",
                "ADMIN",
                "Ativo tombamento " + asset.getTagNumber() + " inativado"
        );
    }
}