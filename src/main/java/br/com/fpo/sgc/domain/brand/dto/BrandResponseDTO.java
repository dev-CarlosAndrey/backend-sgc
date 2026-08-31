package br.com.fpo.sgc.domain.brand.dto;

import br.com.fpo.sgc.domain.brand.Brand;

public record BrandResponseDTO(
        Long id,
        String name,
        Boolean active
) {
    public static BrandResponseDTO fromEntity(Brand brand) {
        return new BrandResponseDTO(
                brand.getId(),
                brand.getName(),
                brand.getActive()
        );
    }
}