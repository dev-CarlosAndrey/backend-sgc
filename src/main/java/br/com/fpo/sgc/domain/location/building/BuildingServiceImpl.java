package br.com.fpo.sgc.domain.location.building;

import br.com.fpo.sgc.common.exception.BusinessException;
import br.com.fpo.sgc.domain.company.Company;
import br.com.fpo.sgc.domain.company.CompanyRepository;
import br.com.fpo.sgc.domain.location.building.dto.BuildingRequestDTO;
import br.com.fpo.sgc.domain.location.building.dto.BuildingResponseDTO;
import br.com.fpo.sgc.domain.location.building.mapper.BuildingMapper;
import br.com.fpo.sgc.domain.location.sector.SectorRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BuildingServiceImpl implements BuildingService {

    private final BuildingRepository buildingRepository;
    private final CompanyRepository companyRepository;
    private final SectorRepository sectorRepository;
    private final BuildingMapper buildingMapper; // Injeção do Mapper

    @Override
    @Transactional
    public BuildingResponseDTO create(BuildingRequestDTO dto) {
        Company company = companyRepository.findById(dto.companyId())
                .orElseThrow(() -> new BusinessException("Empresa não encontrada com o ID: " + dto.companyId()));

        Long nextId = buildingRepository.count() + 1;
        String generatedCode = String.format("BUILD-%03d", nextId);

        Building building = Building.builder()
                .name(dto.name())
                .code(generatedCode)
                .address(dto.address())
                .company(company)
                .active(true)
                .build();

        Building saved = buildingRepository.save(building);
        return buildingMapper.toDTO(saved, 0);
    }

    @Override
    @Transactional(readOnly = true)
    public BuildingResponseDTO findById(Long id) {
        Building building = buildingRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new EntityNotFoundException("Prédio não encontrado com o ID: " + id));

        long sectorsCount = sectorRepository.countByBuildingId(building.getId());
        return buildingMapper.toDTO(building, sectorsCount);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BuildingResponseDTO> findAll(Pageable pageable) {
        return buildingRepository.findByActiveTrue(pageable)
                .map(building -> {
                    long sectorsCount = sectorRepository.countByBuildingId(building.getId());
                    return buildingMapper.toDTO(building, sectorsCount);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BuildingResponseDTO> findByCompany(Long companyId, Pageable pageable) {
        return buildingRepository.findByCompanyIdAndActiveTrue(companyId, pageable)
                .map(building -> {
                    long sectorsCount = sectorRepository.countByBuildingId(building.getId());
                    return buildingMapper.toDTO(building, sectorsCount);
                });
    }

    @Override
    @Transactional
    public BuildingResponseDTO update(Long id, BuildingRequestDTO dto) {
        Building building = buildingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Prédio não encontrado com o ID: " + id));

        Company company = companyRepository.findById(dto.companyId())
                .orElseThrow(() -> new BusinessException("Empresa não encontrada com o ID: " + dto.companyId()));

        building.setName(dto.name());
        building.setAddress(dto.address());
        building.setCompany(company);

        Building updated = buildingRepository.save(building);
        long sectorsCount = sectorRepository.countByBuildingId(updated.getId());
        return buildingMapper.toDTO(updated, sectorsCount);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Building building = buildingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Prédio não encontrado com o ID: " + id));

        building.setActive(false);
        buildingRepository.save(building);
    }
}