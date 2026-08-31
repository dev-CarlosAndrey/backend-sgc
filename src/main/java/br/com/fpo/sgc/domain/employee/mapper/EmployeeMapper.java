package br.com.fpo.sgc.domain.employee.mapper;

import br.com.fpo.sgc.domain.company.Company;
import br.com.fpo.sgc.domain.employee.Employee;
import br.com.fpo.sgc.domain.employee.dto.EmployeeRequestDTO;
import br.com.fpo.sgc.domain.employee.dto.EmployeeResponseDTO;
import br.com.fpo.sgc.domain.location.sector.Sector;
import org.springframework.stereotype.Component;

@Component
public class EmployeeMapper {

    public Employee toEntity(EmployeeRequestDTO dto, Company company, Sector sector) {
        if (dto == null) return null;

        return Employee.builder()
                .name(dto.name())
                .cpf(dto.cpf())
                .registrationNumber(dto.registrationNumber())
                .email(dto.email())
                .phone(dto.phone())
                .position(dto.position())
                .admissionDate(dto.admissionDate())
                .company(company)
                .sector(sector)
                .build();
    }

    public EmployeeResponseDTO toDTO(Employee employee) {
        if (employee == null) return null;

        return new EmployeeResponseDTO(
                employee.getId(),
                employee.getName(),
                employee.getCpf(),
                employee.getRegistrationNumber(),
                employee.getEmail(),
                employee.getPhone(),
                employee.getPosition(),
                employee.getAdmissionDate(),
                employee.getActive(),
                employee.getCompany() != null ? employee.getCompany().getId() : null,
                employee.getCompany() != null ? employee.getCompany().getCompanyName() : null,
                employee.getSector() != null ? employee.getSector().getId() : null,
                employee.getSector() != null ? employee.getSector().getName() : null,
                employee.getCreatedAt()
        );
    }

    public void updateEntityFromDto(Employee employee, EmployeeRequestDTO dto, Company company, Sector sector) {
        if (employee == null || dto == null) return;

        employee.setName(dto.name());
        employee.setCpf(dto.cpf());
        employee.setRegistrationNumber(dto.registrationNumber());
        employee.setEmail(dto.email());
        employee.setPhone(dto.phone());
        employee.setPosition(dto.position());
        employee.setAdmissionDate(dto.admissionDate());
        employee.setCompany(company);
        employee.setSector(sector);
    }
}