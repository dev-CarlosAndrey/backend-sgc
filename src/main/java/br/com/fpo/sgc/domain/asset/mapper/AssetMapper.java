package br.com.fpo.sgc.domain.asset.mapper;

import br.com.fpo.sgc.domain.asset.Asset;
import br.com.fpo.sgc.domain.asset.category.AssetCategory;
import br.com.fpo.sgc.domain.asset.category.mapper.AssetCategoryMapper;
import br.com.fpo.sgc.domain.asset.dto.AssetRequestDTO;
import br.com.fpo.sgc.domain.asset.dto.AssetResponseDTO;
import br.com.fpo.sgc.domain.employee.Employee;
import br.com.fpo.sgc.domain.employee.mapper.EmployeeMapper;
import br.com.fpo.sgc.domain.location.sector.Sector;
import br.com.fpo.sgc.domain.location.sector.mapper.SectorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AssetMapper {

    private final SectorMapper sectorMapper;
    private final AssetCategoryMapper assetCategoryMapper;
    private final EmployeeMapper employeeMapper;

    public Asset toEntity(AssetRequestDTO dto, Sector sector, AssetCategory category, Employee responsible) {
        if (dto == null) return null;

        return Asset.builder()
                .tagNumber(dto.tagNumber())
                .name(dto.name())
                .description(dto.description())
                .model(dto.model())
                .serialNumber(dto.serialNumber())
                .status(dto.status())
                .sector(sector)
                .category(category)
                .responsible(responsible)
                .acquisitionDate(dto.acquisitionDate())
                .acquisitionValue(dto.acquisitionValue())
                .conservationState(dto.conservationState())
                .build();
    }

    public AssetResponseDTO toDTO(Asset asset) {
        if (asset == null) return null;

        return new AssetResponseDTO(
                asset.getId(),
                asset.getTagNumber(),
                asset.getName(),
                asset.getDescription(),
                asset.getModel(),
                asset.getSerialNumber(),
                asset.getStatus(),
                sectorMapper.toDTO(asset.getSector(), 0, 0),
                asset.getCategory() != null ? assetCategoryMapper.toDTO(asset.getCategory()) : null,
                asset.getResponsible() != null ? employeeMapper.toDTO(asset.getResponsible()) : null,
                asset.getAcquisitionDate(),
                asset.getAcquisitionValue(),
                asset.getConservationState(),
                asset.getPhotoUrl(),
                asset.getActive(),
                asset.getCreatedAt()
        );
    }
}
