package br.com.fpo.sgc.domain.asset.category.mapper;

import br.com.fpo.sgc.domain.asset.category.AssetCategory;
import br.com.fpo.sgc.domain.asset.category.dto.AssetCategoryRequestDTO;
import br.com.fpo.sgc.domain.asset.category.dto.AssetCategoryResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class AssetCategoryMapper {

    public AssetCategory toEntity(AssetCategoryRequestDTO dto) {
        if (dto == null) return null;

        return AssetCategory.builder()
                .name(dto.name())
                .description(dto.description())
                .build();
    }

    public AssetCategoryResponseDTO toDTO(AssetCategory category) {
        if (category == null) return null;

        return new AssetCategoryResponseDTO(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getActive(),
                category.getCreatedAt()
        );
    }

    public void updateEntityFromDto(AssetCategory category, AssetCategoryRequestDTO dto) {
        if (category == null || dto == null) return;

        category.setName(dto.name());
        category.setDescription(dto.description());
        if (dto.active() != null) {
            category.setActive(dto.active());
        }
    }
}
