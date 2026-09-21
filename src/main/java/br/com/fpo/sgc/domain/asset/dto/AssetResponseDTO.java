package br.com.fpo.sgc.domain.asset.dto;

import br.com.fpo.sgc.domain.asset.AssetConservationState;
import br.com.fpo.sgc.domain.asset.AssetStatus;
import br.com.fpo.sgc.domain.asset.category.dto.AssetCategoryResponseDTO;
import br.com.fpo.sgc.domain.employee.dto.EmployeeResponseDTO;
import br.com.fpo.sgc.domain.location.sector.dto.SectorResponseDTO;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record AssetResponseDTO(
        Long id,
        String tagNumber,
        String name,
        String description,
        String model,
        String serialNumber,
        AssetStatus status,
        SectorResponseDTO sector,
        AssetCategoryResponseDTO category,
        EmployeeResponseDTO responsible,
        LocalDate acquisitionDate,
        BigDecimal acquisitionValue,
        AssetConservationState conservationState,
        String photoUrl,
        Boolean active,
        Instant createdAt
) {
}
