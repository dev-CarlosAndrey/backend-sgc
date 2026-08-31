package br.com.fpo.sgc.domain.material.mapper;

import br.com.fpo.sgc.domain.material.Material;
import br.com.fpo.sgc.domain.material.dto.MaterialRequestDTO;
import br.com.fpo.sgc.domain.material.dto.MaterialResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class MaterialMapper {

    public Material toEntity(MaterialRequestDTO dto) {
        if (dto == null) return null;

        return Material.builder()
                .name(dto.name())
                .category(dto.category())
                .build();
    }

    public MaterialResponseDTO toDTO(Material material) {
        if (material == null) return null;

        return new MaterialResponseDTO(
                material.getId(),
                material.getName(),
                material.getCategory(),
                material.getActive(),
                material.getCreatedAt()
        );
    }

    public void updateEntityFromDto(Material material, MaterialRequestDTO dto) {
        if (material == null || dto == null) return;

        material.setName(dto.name());
        material.setCategory(dto.category());
        if (dto.active() != null) {
            material.setActive(dto.active());
        }
    }
}