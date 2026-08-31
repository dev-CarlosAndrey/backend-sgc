package br.com.fpo.sgc.domain.company.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CompanyRequestDTO(
        @NotBlank(message = "O nome empresarial é obrigatório")
        String companyName,

        @NotBlank(message = "O CNPJ é obrigatório")
        String cnpj,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "E-mail inválido")
        String email,

        @NotBlank(message = "O telefone é obrigatório")
        String phone,

        String logoUrl,

        Boolean active
) {}