package br.com.fpo.sgc.domain.asset.category;

import br.com.fpo.sgc.common.exception.BusinessException;
import br.com.fpo.sgc.domain.asset.category.dto.AssetCategoryRequestDTO;
import br.com.fpo.sgc.domain.asset.category.dto.AssetCategoryResponseDTO;
import br.com.fpo.sgc.domain.asset.category.mapper.AssetCategoryMapper;
import br.com.fpo.sgc.domain.audit.AuditLogService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AssetCategoryServiceImpl implements AssetCategoryService {

    private final AssetCategoryRepository assetCategoryRepository;
    private final AssetCategoryMapper assetCategoryMapper;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public AssetCategoryResponseDTO create(AssetCategoryRequestDTO dto) {
        if (assetCategoryRepository.existsByNameIgnoreCase(dto.name())) {
            throw new BusinessException("Já existe uma categoria de bem cadastrada com o nome: " + dto.name());
        }

        AssetCategory category = assetCategoryMapper.toEntity(dto);
        AssetCategory savedCategory = assetCategoryRepository.save(category);

        auditLogService.log(
                "AssetCategory",
                savedCategory.getId(),
                "CREATE",
                "ADMIN",
                "Categoria de bem " + savedCategory.getName() + " criada"
        );

        return assetCategoryMapper.toDTO(savedCategory);
    }

    @Override
    @Transactional(readOnly = true)
    public AssetCategoryResponseDTO findById(Long id) {
        AssetCategory category = assetCategoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoria de bem não encontrada com ID: " + id));

        return assetCategoryMapper.toDTO(category);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssetCategoryResponseDTO> findAll(Pageable pageable) {
        return assetCategoryRepository.findByActiveTrue(pageable)
                .map(assetCategoryMapper::toDTO);
    }

    @Override
    @Transactional
    public AssetCategoryResponseDTO update(Long id, AssetCategoryRequestDTO dto) {
        AssetCategory category = assetCategoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoria de bem não encontrada com ID: " + id));

        if (!category.getName().equalsIgnoreCase(dto.name()) && assetCategoryRepository.existsByNameIgnoreCase(dto.name())) {
            throw new BusinessException("Já existe uma categoria de bem cadastrada com o nome: " + dto.name());
        }

        assetCategoryMapper.updateEntityFromDto(category, dto);
        AssetCategory updatedCategory = assetCategoryRepository.save(category);

        auditLogService.log(
                "AssetCategory",
                updatedCategory.getId(),
                "UPDATE",
                "ADMIN",
                "Categoria de bem " + updatedCategory.getName() + " atualizada"
        );

        return assetCategoryMapper.toDTO(updatedCategory);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        AssetCategory category = assetCategoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoria de bem não encontrada com ID: " + id));

        category.setActive(false);
        assetCategoryRepository.save(category);

        auditLogService.log(
                "AssetCategory",
                id,
                "DELETE",
                "ADMIN",
                "Categoria de bem " + category.getName() + " inativada"
        );
    }
}
