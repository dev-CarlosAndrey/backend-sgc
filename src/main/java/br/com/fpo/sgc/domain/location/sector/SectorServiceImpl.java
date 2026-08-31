package br.com.fpo.sgc.domain.location.sector;

import br.com.fpo.sgc.domain.audit.AuditLogService;
import br.com.fpo.sgc.domain.employee.Employee;
import br.com.fpo.sgc.domain.employee.EmployeeRepository;
import br.com.fpo.sgc.domain.location.building.Building;
import br.com.fpo.sgc.domain.location.building.BuildingRepository;
import br.com.fpo.sgc.domain.location.sector.dto.SectorRequestDTO;
import br.com.fpo.sgc.domain.location.sector.dto.SectorResponseDTO;
import br.com.fpo.sgc.domain.location.sector.mapper.SectorMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SectorServiceImpl implements SectorService {

    private final SectorRepository sectorRepository;
    private final BuildingRepository buildingRepository;
    private final EmployeeRepository employeeRepository;
    private final AuditLogService auditLogService;
    private final SectorMapper sectorMapper;

    @Override
    @Transactional
    public SectorResponseDTO create(SectorRequestDTO dto) {
        Building building = buildingRepository.findById(dto.buildingId())
                .orElseThrow(() -> new EntityNotFoundException("Prédio não encontrado com ID: " + dto.buildingId()));

        Employee manager = null;
        if (dto.managerId() != null) {
            manager = employeeRepository.findById(dto.managerId())
                    .orElseThrow(() -> new EntityNotFoundException("Funcionário responsável não encontrado com ID: " + dto.managerId()));
        }

        Sector sector = sectorMapper.toEntity(dto, building, manager);
        Sector savedSector = sectorRepository.save(sector);

        auditLogService.log(
                "Sector",
                savedSector.getId(),
                "CREATE",
                savedSector.getName(),
                "Setor " + savedSector.getName() + " criado no prédio " + building.getName()
        );

        return mapToDTO(savedSector);
    }

    @Override
    @Transactional(readOnly = true)
    public SectorResponseDTO findById(Long id) {
        Sector sector = sectorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Setor não encontrado com ID: " + id));

        return mapToDTO(sector);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SectorResponseDTO> findAll(Pageable pageable) {
        return sectorRepository.findByActiveTrue(pageable)
                .map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SectorResponseDTO> findByBuildingId(Long buildingId, Pageable pageable) {
        return sectorRepository.findByBuildingIdAndActiveTrue(buildingId, pageable)
                .map(this::mapToDTO);
    }

    @Override
    @Transactional
    public SectorResponseDTO update(Long id, SectorRequestDTO dto) {
        Sector sector = sectorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Setor não encontrado com ID: " + id));

        Building building = buildingRepository.findById(dto.buildingId())
                .orElseThrow(() -> new EntityNotFoundException("Prédio não encontrado com ID: " + dto.buildingId()));

        Employee manager = null;
        if (dto.managerId() != null) {
            manager = employeeRepository.findById(dto.managerId())
                    .orElseThrow(() -> new EntityNotFoundException("Funcionário responsável não encontrado com ID: " + dto.managerId()));
        }

        sector.setName(dto.name());
        sector.setBuilding(building);
        sector.setManager(manager);
        if (dto.active() != null) {
            sector.setActive(dto.active());
        }

        Sector updatedSector = sectorRepository.save(sector);

        auditLogService.log(
                "Sector",
                updatedSector.getId(),
                "UPDATE",
                updatedSector.getName(),
                "Setor " + updatedSector.getName() + " atualizado"
        );

        return mapToDTO(updatedSector);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Sector sector = sectorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Setor não encontrado com ID: " + id));

        sector.setActive(false);
        sectorRepository.save(sector);

        auditLogService.log(
                "Sector",
                id,
                "DELETE",
                sector.getName(),
                "Setor " + sector.getName() + " inativado"
        );
    }

    private SectorResponseDTO mapToDTO(Sector sector) {
        long totalAssets = 0;
        long activeTickets = 0;

        return sectorMapper.toDTO(sector, totalAssets, activeTickets);
    }
}