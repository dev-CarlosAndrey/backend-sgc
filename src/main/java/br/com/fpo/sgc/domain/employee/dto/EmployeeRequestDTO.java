package br.com.fpo.sgc.domain.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record EmployeeRequestDTO(
        @NotBlank(message = "O nome é obrigatório")
        String name,

        @NotBlank(message = "O CPF é obrigatório")
        String cpf,

        @NotBlank(message = "A matrícula é obrigatória")
        String registrationNumber,

        String email,
        String phone,
        String position,
        LocalDate admissionDate,

        @NotNull(message = "A empresa é obrigatória")
        Long companyId,

        Long sectorId
) {}