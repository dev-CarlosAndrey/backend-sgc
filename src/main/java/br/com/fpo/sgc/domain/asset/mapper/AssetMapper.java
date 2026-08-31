package br.com.fpo.sgc.domain.asset.mapper;

import br.com.fpo.sgc.domain.asset.Asset;
import br.com.fpo.sgc.domain.asset.dto.AssetRequestDTO;
import br.com.fpo.sgc.domain.asset.dto.AssetResponseDTO;
import br.com.fpo.sgc.domain.location.sector.Sector;
import br.com.fpo.sgc.domain.location.sector.mapper.SectorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AssetMapper {

    private final SectorMapper sectorMapper;

    public Asset toEntity(AssetRequestDTO dto, Sector sector) {
        if (dto == null) return null;

        return Asset.builder()
                .tagNumber(dto.tagNumber())
                .name(dto.name())
                .description(dto.description())
                .model(dto.model())
                .serialNumber(dto.serialNumber())
                .status(dto.status())
                .sector(sector)
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
                asset.getActive(),
                asset.getCreatedAt()
        );
    }
}