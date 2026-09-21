package br.com.fpo.sgc.domain.asset.dto;

import br.com.fpo.sgc.domain.asset.AssetConservationState;
import br.com.fpo.sgc.domain.asset.AssetStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AssetRequestDTO(
        @NotBlank(message = "O número de tombamento/patrimônio é obrigatório")
        @Size(max = 50, message = "O tombamento deve ter no máximo 50 caracteres")
        String tagNumber,

        @NotBlank(message = "O nome do ativo é obrigatório")
        @Size(min = 2, max = 120, message = "O nome deve ter entre 2 e 120 caracteres")
        String name,

        String description,
        String model,
        String serialNumber,

        @NotNull(message = "O status do ativo é obrigatório")
        AssetStatus status,

        @NotNull(message = "O ID do setor é obrigatório")
        Long sectorId,

        Long categoryId,

        Long responsibleId,

        @PastOrPresent(message = "A data de aquisição não pode ser futura")
        LocalDate acquisitionDate,

        @DecimalMin(value = "0.0", inclusive = true, message = "O valor de aquisição não pode ser negativo")
        BigDecimal acquisitionValue,

        AssetConservationState conservationState
) {}
