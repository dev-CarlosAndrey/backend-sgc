package br.com.fpo.sgc.domain.serviceorder.mapper;

import br.com.fpo.sgc.domain.asset.Asset;
import br.com.fpo.sgc.domain.asset.mapper.AssetMapper;
import br.com.fpo.sgc.domain.location.sector.Sector;
import br.com.fpo.sgc.domain.location.sector.mapper.SectorMapper;
import br.com.fpo.sgc.domain.serviceorder.ServiceOrder;
import br.com.fpo.sgc.domain.serviceorder.dto.ServiceOrderRequestDTO;
import br.com.fpo.sgc.domain.serviceorder.dto.ServiceOrderResponseDTO;
import br.com.fpo.sgc.domain.user.User;
import br.com.fpo.sgc.domain.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ServiceOrderMapper {

    private final UserMapper userMapper;
    private final AssetMapper assetMapper;
    private final SectorMapper sectorMapper;

    public ServiceOrder toEntity(ServiceOrderRequestDTO dto, User requester, Sector sector, Asset asset) {
        if (dto == null) return null;

        return ServiceOrder.builder()
                .title(dto.title())
                .description(dto.description())
                .priority(dto.priority())
                .requester(requester)
                .sector(sector)
                .asset(asset)
                .build();
    }

    public ServiceOrderResponseDTO toDTO(ServiceOrder so) {
        if (so == null) return null;

        return new ServiceOrderResponseDTO(
                so.getId(),
                so.getTitle(),
                so.getDescription(),
                so.getStatus(),
                so.getPriority(),
                userMapper.toDTO(so.getRequester()),
                so.getTechnician() != null ? userMapper.toDTO(so.getTechnician()) : null,
                so.getAsset() != null ? assetMapper.toDTO(so.getAsset()) : null,
                sectorMapper.toDTO(so.getSector(), 0, 0),
                so.getSolutionDetails(),
                so.getClosedAt(),
                so.getActive(),
                so.getCreatedAt()
        );
    }
}