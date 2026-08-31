package br.com.fpo.sgc.domain.location.sector.mapper;

import br.com.fpo.sgc.domain.employee.Employee;
import br.com.fpo.sgc.domain.location.building.Building;
import br.com.fpo.sgc.domain.location.building.mapper.BuildingMapper;
import br.com.fpo.sgc.domain.location.sector.Sector;
import br.com.fpo.sgc.domain.location.sector.dto.SectorRequestDTO;
import br.com.fpo.sgc.domain.location.sector.dto.SectorResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SectorMapper {

    private final BuildingMapper buildingMapper;

    public Sector toEntity(SectorRequestDTO dto, Building building, Employee manager) {
        if (dto == null) return null;

        return Sector.builder()
                .name(dto.name())
                .building(building)
                .manager(manager)
                .build();
    }

    public SectorResponseDTO toDTO(Sector sector, long totalAssets, long activeTickets) {
        if (sector == null) return null;

        return new SectorResponseDTO(
                sector.getId(),
                sector.getName(),
                buildingMapper.toDTO(sector.getBuilding(), 0),
                sector.getManager() != null ? sector.getManager().getId() : null,
                sector.getManager() != null ? sector.getManager().getName() : null,
                null,
                totalAssets,
                activeTickets,
                sector.getActive(),
                sector.getCreatedAt()
        );
    }
}