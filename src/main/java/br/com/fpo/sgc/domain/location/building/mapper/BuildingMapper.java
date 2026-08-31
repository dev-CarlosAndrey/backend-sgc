package br.com.fpo.sgc.domain.location.building.mapper;

import br.com.fpo.sgc.domain.location.building.Building;
import br.com.fpo.sgc.domain.location.building.dto.BuildingResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class BuildingMapper {

    public BuildingResponseDTO toDTO(Building building, long linkedSectorsCount) {
        if (building == null) return null;

        return new BuildingResponseDTO(
                building.getId(),
                building.getName(),
                building.getCode(),
                building.getAddress(),
                building.getCompany() != null ? building.getCompany().getId() : null,
                building.getCompany() != null ? building.getCompany().getCompanyName() : null,
                linkedSectorsCount
        );
    }

    public BuildingResponseDTO toDTO(Building building) {
        return toDTO(building, 0);
    }
}