package br.com.fpo.sgc.domain.company.mapper;

import br.com.fpo.sgc.domain.company.Company;
import br.com.fpo.sgc.domain.company.dto.CompanyRequestDTO;
import br.com.fpo.sgc.domain.company.dto.CompanyResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class CompanyMapper {

    public Company toEntity(CompanyRequestDTO dto) {
        if (dto == null) {
            return null;
        }

        return Company.builder()
                .companyName(dto.companyName())
                .cnpj(dto.cnpj())
                .email(dto.email())
                .phone(dto.phone())
                .logoUrl(dto.logoUrl())
                .active(dto.active() != null ? dto.active() : true)
                .build();
    }

    public CompanyResponseDTO toDTO(Company company, long totalBuildings, long totalSectors) {
        if (company == null) {
            return null;
        }

        return new CompanyResponseDTO(
                company.getId(),
                company.getCompanyName(),
                company.getCnpj(),
                company.getEmail(),
                company.getPhone(),
                company.getLogoUrl(),
                company.getActive(),
                totalBuildings,
                totalSectors
        );
    }
}