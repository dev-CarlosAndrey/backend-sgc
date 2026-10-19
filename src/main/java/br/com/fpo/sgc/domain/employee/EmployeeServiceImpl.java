package br.com.fpo.sgc.domain.employee;

import br.com.fpo.sgc.common.exception.BusinessException;
import br.com.fpo.sgc.common.util.SearchTerm;
import br.com.fpo.sgc.domain.audit.AuditLogService;
import br.com.fpo.sgc.domain.company.Company;
import br.com.fpo.sgc.domain.company.CompanyRepository;
import br.com.fpo.sgc.domain.employee.dto.EmployeeRequestDTO;
import br.com.fpo.sgc.domain.employee.dto.EmployeeResponseDTO;
import br.com.fpo.sgc.domain.employee.mapper.EmployeeMapper;
import br.com.fpo.sgc.domain.location.sector.Sector;
import br.com.fpo.sgc.domain.location.sector.SectorRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;
    private final SectorRepository sectorRepository;
    private final EmployeeMapper employeeMapper;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public EmployeeResponseDTO create(EmployeeRequestDTO dto) {
        if (employeeRepository.existsByCpf(dto.cpf())) {
            throw new BusinessException("Já existe um funcionário cadastrado com este CPF.");
        }

        if (employeeRepository.existsByRegistrationNumber(dto.registrationNumber())) {
            throw new BusinessException("Já existe um funcionário cadastrado com esta matrícula.");
        }

        Company company = companyRepository.findById(dto.companyId())
                .orElseThrow(() -> new EntityNotFoundException("Empresa não encontrada com ID: " + dto.companyId()));

        Sector sector = null;
        if (dto.sectorId() != null) {
            sector = sectorRepository.findById(dto.sectorId())
                    .orElseThrow(() -> new EntityNotFoundException("Setor não encontrado com ID: " + dto.sectorId()));
        }

        Employee employee = employeeMapper.toEntity(dto, company, sector);
        Employee savedEmployee = employeeRepository.save(employee);

        auditLogService.log(
                "Employee",
                savedEmployee.getId(),
                "CREATE",
                savedEmployee.getName(),
                "Funcionário " + savedEmployee.getName() + " cadastrado"
        );

        return employeeMapper.toDTO(savedEmployee);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeResponseDTO> findAll(String q, Long sectorId, Boolean active, Pageable pageable) {
        return employeeRepository.search(SearchTerm.toLikePattern(q), sectorId, active, pageable)
                .map(employeeMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponseDTO findById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Funcionário não encontrado com ID: " + id));
        return employeeMapper.toDTO(employee);
    }

    @Override
    @Transactional
    public EmployeeResponseDTO update(Long id, EmployeeRequestDTO dto) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Funcionário não encontrado com ID: " + id));

        Company company = companyRepository.findById(dto.companyId())
                .orElseThrow(() -> new EntityNotFoundException("Empresa não encontrada com ID: " + dto.companyId()));

        Sector sector = null;
        if (dto.sectorId() != null) {
            sector = sectorRepository.findById(dto.sectorId())
                    .orElseThrow(() -> new EntityNotFoundException("Setor não encontrado com ID: " + dto.sectorId()));
        }

        employeeMapper.updateEntityFromDto(employee, dto, company, sector);
        Employee updatedEmployee = employeeRepository.save(employee);

        auditLogService.log(
                "Employee",
                updatedEmployee.getId(),
                "UPDATE",
                updatedEmployee.getName(),
                "Funcionário " + updatedEmployee.getName() + " atualizado"
        );

        return employeeMapper.toDTO(updatedEmployee);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Funcionário não encontrado com ID: " + id));

        employee.setActive(false);
        employeeRepository.save(employee);

        auditLogService.log(
                "Employee",
                id,
                "DELETE",
                employee.getName(),
                "Funcionário " + employee.getName() + " inativado"
        );
    }
}