package br.com.fpo.sgc.domain.company;

import br.com.fpo.sgc.common.exception.BusinessException;
import br.com.fpo.sgc.common.storage.FileStorageService;
import br.com.fpo.sgc.domain.audit.AuditLogService;
import br.com.fpo.sgc.domain.company.dto.CompanyRequestDTO;
import br.com.fpo.sgc.domain.company.dto.CompanyResponseDTO;
import br.com.fpo.sgc.domain.company.mapper.CompanyMapper;
import br.com.fpo.sgc.domain.location.building.BuildingRepository;
import br.com.fpo.sgc.domain.location.sector.SectorRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

    private static final Set<String> ALLOWED_LOGO_TYPES = Set.of("image/png", "image/jpeg", "image/svg+xml");
    private static final long MAX_LOGO_SIZE_BYTES = 2L * 1024 * 1024;

    private final CompanyRepository companyRepository;
    private final BuildingRepository buildingRepository;
    private final SectorRepository sectorRepository;
    private final AuditLogService auditLogService;
    private final CompanyMapper companyMapper;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public CompanyResponseDTO create(CompanyRequestDTO dto) {
        if (companyRepository.existsByCnpj(dto.cnpj())) {
            throw new BusinessException("Já existe uma empresa cadastrada com o CNPJ: " + dto.cnpj());
        }

        Company company = companyMapper.toEntity(dto);
        Company savedCompany = companyRepository.save(company);

        auditLogService.log(
                "Company",
                savedCompany.getId(),
                "CREATE",
                savedCompany.getCompanyName(),
                "Empresa " + savedCompany.getCompanyName() + " cadastrada"
        );

        return mapToDTO(savedCompany);
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyResponseDTO findById(Long id) {
        Company company = companyRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new EntityNotFoundException("Empresa não encontrada com o ID: " + id));

        return mapToDTO(company);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CompanyResponseDTO> findAll(Pageable pageable) {
        return companyRepository.findByActiveTrue(pageable)
                .map(this::mapToDTO);
    }

    @Override
    @Transactional
    public CompanyResponseDTO update(Long id, CompanyRequestDTO dto) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Empresa não encontrada com o ID: " + id));

        if (companyRepository.existsByCnpjAndIdNot(dto.cnpj(), id)) {
            throw new BusinessException("Já existe outra empresa cadastrada com o CNPJ: " + dto.cnpj());
        }

        company.setCompanyName(dto.companyName());
        company.setCnpj(dto.cnpj());
        company.setEmail(dto.email());
        company.setPhone(dto.phone());
        if (dto.logoUrl() != null) {
            company.setLogoUrl(dto.logoUrl());
        }
        if (dto.active() != null) {
            company.setActive(dto.active());
        }

        Company updatedCompany = companyRepository.save(company);

        auditLogService.log(
                "Company",
                updatedCompany.getId(),
                "UPDATE",
                updatedCompany.getCompanyName(),
                "Empresa " + updatedCompany.getCompanyName() + " atualizada"
        );

        return mapToDTO(updatedCompany);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Empresa não encontrada com o ID: " + id));

        company.setActive(false);
        companyRepository.save(company);

        auditLogService.log(
                "Company",
                id,
                "DELETE",
                company.getCompanyName(),
                "Empresa " + company.getCompanyName() + " inativada"
        );
    }

    @Override
    @Transactional
    public CompanyResponseDTO uploadLogo(Long id, MultipartFile file) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Empresa não encontrada com o ID: " + id));

        String logoUrl = fileStorageService.store(file, "logos", ALLOWED_LOGO_TYPES, MAX_LOGO_SIZE_BYTES);
        company.setLogoUrl(logoUrl);

        Company updatedCompany = companyRepository.save(company);

        auditLogService.log(
                "Company",
                updatedCompany.getId(),
                "UPDATE",
                updatedCompany.getCompanyName(),
                "Logo da empresa " + updatedCompany.getCompanyName() + " atualizado"
        );

        return mapToDTO(updatedCompany);
    }

    /**
     * Busca os totais atualizados no repositório e chama o Mapper.
     */
    private CompanyResponseDTO mapToDTO(Company company) {
        long totalBuildings = buildingRepository.countByCompanyIdAndActiveTrue(company.getId());
        long totalSectors = sectorRepository.countByBuilding_CompanyIdAndActiveTrue(company.getId());

        return companyMapper.toDTO(company, totalBuildings, totalSectors);
    }
}