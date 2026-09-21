package br.com.fpo.sgc.domain.asset;

import br.com.fpo.sgc.common.exception.BusinessException;
import br.com.fpo.sgc.common.storage.FileStorageService;
import br.com.fpo.sgc.domain.asset.category.AssetCategory;
import br.com.fpo.sgc.domain.asset.category.AssetCategoryRepository;
import br.com.fpo.sgc.domain.asset.dto.AssetRequestDTO;
import br.com.fpo.sgc.domain.asset.dto.AssetResponseDTO;
import br.com.fpo.sgc.domain.asset.mapper.AssetMapper;
import br.com.fpo.sgc.domain.audit.AuditLogService;
import br.com.fpo.sgc.domain.employee.Employee;
import br.com.fpo.sgc.domain.employee.EmployeeRepository;
import br.com.fpo.sgc.domain.location.sector.Sector;
import br.com.fpo.sgc.domain.location.sector.SectorRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AssetServiceImpl implements AssetService {

    private static final Set<String> ALLOWED_PHOTO_TYPES = Set.of("image/png", "image/jpeg", "image/webp");
    private static final long MAX_PHOTO_SIZE_BYTES = 5L * 1024 * 1024;

    private final AssetRepository assetRepository;
    private final SectorRepository sectorRepository;
    private final AssetCategoryRepository assetCategoryRepository;
    private final EmployeeRepository employeeRepository;
    private final AuditLogService auditLogService;
    private final AssetMapper assetMapper;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public AssetResponseDTO create(AssetRequestDTO dto) {
        if (assetRepository.existsByTagNumber(dto.tagNumber())) {
            throw new BusinessException("Já existe um ativo cadastrado com o tombamento: " + dto.tagNumber());
        }

        Sector sector = sectorRepository.findById(dto.sectorId())
                .orElseThrow(() -> new EntityNotFoundException("Setor não encontrado com ID: " + dto.sectorId()));

        AssetCategory category = resolveCategory(dto.categoryId());
        Employee responsible = resolveResponsible(dto.responsibleId());

        Asset asset = assetMapper.toEntity(dto, sector, category, responsible);
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
    @Transactional(readOnly = true)
    public Page<AssetResponseDTO> findByStatus(AssetStatus status, Pageable pageable) {
        return assetRepository.findByStatusAndActiveTrue(status, pageable).map(assetMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssetResponseDTO> findByCategoryId(Long categoryId, Pageable pageable) {
        return assetRepository.findByCategoryIdAndActiveTrue(categoryId, pageable).map(assetMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssetResponseDTO> findByResponsibleId(Long responsibleId, Pageable pageable) {
        return assetRepository.findByResponsibleIdAndActiveTrue(responsibleId, pageable).map(assetMapper::toDTO);
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

        AssetCategory category = resolveCategory(dto.categoryId());
        Employee responsible = resolveResponsible(dto.responsibleId());

        asset.setTagNumber(dto.tagNumber());
        asset.setName(dto.name());
        asset.setDescription(dto.description());
        asset.setModel(dto.model());
        asset.setSerialNumber(dto.serialNumber());
        asset.setStatus(dto.status());
        asset.setSector(sector);
        asset.setCategory(category);
        asset.setResponsible(responsible);
        asset.setAcquisitionDate(dto.acquisitionDate());
        asset.setAcquisitionValue(dto.acquisitionValue());
        asset.setConservationState(dto.conservationState());

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

    @Override
    @Transactional
    public AssetResponseDTO uploadPhoto(Long id, MultipartFile file) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ativo não encontrado com ID: " + id));

        String photoUrl = fileStorageService.store(file, "assets", ALLOWED_PHOTO_TYPES, MAX_PHOTO_SIZE_BYTES);
        asset.setPhotoUrl(photoUrl);

        Asset updatedAsset = assetRepository.save(asset);

        // Registrar auditoria
        auditLogService.log(
                "Asset",
                updatedAsset.getId(),
                "UPDATE",
                "ADMIN",
                "Foto do ativo " + updatedAsset.getTagNumber() + " atualizada"
        );

        return assetMapper.toDTO(updatedAsset);
    }

    private AssetCategory resolveCategory(Long categoryId) {
        if (categoryId == null) return null;

        return assetCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Categoria de bem não encontrada com ID: " + categoryId));
    }

    private Employee resolveResponsible(Long responsibleId) {
        if (responsibleId == null) return null;

        return employeeRepository.findById(responsibleId)
                .orElseThrow(() -> new EntityNotFoundException("Funcionário responsável não encontrado com ID: " + responsibleId));
    }
}
