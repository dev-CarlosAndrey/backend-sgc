package br.com.fpo.sgc.domain.serviceorder.dto;

import br.com.fpo.sgc.domain.serviceorder.ServiceOrderStatus;
import jakarta.validation.constraints.NotNull;

public record ServiceOrderStatusChangeRequestDTO(
        @NotNull(message = "O novo status é obrigatório")
        ServiceOrderStatus status,
        String note
) {
}
