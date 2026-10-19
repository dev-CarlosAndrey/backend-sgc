package br.com.fpo.sgc.domain.material;

import br.com.fpo.sgc.common.util.SearchTerm;
import br.com.fpo.sgc.domain.audit.AuditLogService;
import br.com.fpo.sgc.domain.material.dto.MaterialRequestDTO;
import br.com.fpo.sgc.domain.material.dto.MaterialResponseDTO;
import br.com.fpo.sgc.domain.material.mapper.MaterialMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MaterialServiceImpl implements MaterialService {

    private final MaterialRepository materialRepository;
    private final MaterialMapper materialMapper;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public MaterialResponseDTO create(MaterialRequestDTO dto) {
        Material material = materialMapper.toEntity(dto);
        Material savedMaterial = materialRepository.save(material);

        auditLogService.log(
                "Material",
                savedMaterial.getId(),
                "CREATE",
                savedMaterial.getName(),
                "Material " + savedMaterial.getName() + " criado com categoria " + savedMaterial.getCategory()
        );

        return materialMapper.toDTO(savedMaterial);
    }

    @Override
    @Transactional(readOnly = true)
    public MaterialResponseDTO findById(Long id) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Material não encontrado com ID: " + id));

        return materialMapper.toDTO(material);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MaterialResponseDTO> findAll(String q, Pageable pageable) {
        return materialRepository.search(SearchTerm.toLikePattern(q), pageable)
                .map(materialMapper::toDTO);
    }

    @Override
    @Transactional
    public MaterialResponseDTO update(Long id, MaterialRequestDTO dto) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Material não encontrado com ID: " + id));

        materialMapper.updateEntityFromDto(material, dto);
        Material updatedMaterial = materialRepository.save(material);

        auditLogService.log(
                "Material",
                updatedMaterial.getId(),
                "UPDATE",
                updatedMaterial.getName(),
                "Material " + updatedMaterial.getName() + " atualizado"
        );

        return materialMapper.toDTO(updatedMaterial);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Material não encontrado com ID: " + id));

        material.setActive(false);
        materialRepository.save(material);

        auditLogService.log(
                "Material",
                id,
                "DELETE",
                material.getName(),
                "Material " + material.getName() + " inativado"
        );
    }
}