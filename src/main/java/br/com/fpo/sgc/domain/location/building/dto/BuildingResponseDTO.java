package br.com.fpo.sgc.domain.location.building.dto;

public record BuildingResponseDTO(
        Long id,
        String name,
        String code,
        String address,
        Long companyId,
        String companyName,
        long linkedSectorsCount
) {
}