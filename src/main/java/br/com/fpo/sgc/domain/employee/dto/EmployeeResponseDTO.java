package br.com.fpo.sgc.domain.employee.dto;

import br.com.fpo.sgc.domain.employee.Employee;

import java.time.Instant;
import java.time.LocalDate;

public record EmployeeResponseDTO(
        Long id,
        String name,
        String cpf,
        String registrationNumber,
        String email,
        String phone,
        String position,
        LocalDate admissionDate,
        Boolean active,
        Long companyId,
        String companyName,
        Long sectorId,
        String sectorName,
        Instant createdAt
) {
}