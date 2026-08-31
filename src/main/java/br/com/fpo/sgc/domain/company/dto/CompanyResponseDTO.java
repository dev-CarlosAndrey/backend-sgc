package br.com.fpo.sgc.domain.company.dto;

public record CompanyResponseDTO(
        Long id,
        String companyName,
        String cnpj,
        String email,
        String phone,
        String logoUrl,
        Boolean active,
        long totalBuildings,
        long totalSectors
) {
}