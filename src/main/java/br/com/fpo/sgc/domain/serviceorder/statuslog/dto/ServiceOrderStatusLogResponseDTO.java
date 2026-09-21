package br.com.fpo.sgc.domain.serviceorder.statuslog.dto;

import br.com.fpo.sgc.domain.serviceorder.ServiceOrderStatus;
import br.com.fpo.sgc.domain.serviceorder.statuslog.ServiceOrderStatusLog;

import java.time.Instant;

public record ServiceOrderStatusLogResponseDTO(
        Long id,
        Long serviceOrderId,
        ServiceOrderStatus previousStatus,
        ServiceOrderStatus newStatus,
        String changedBy,
        String note,
        Instant createdAt
) {
    public static ServiceOrderStatusLogResponseDTO fromEntity(ServiceOrderStatusLog log) {
        return new ServiceOrderStatusLogResponseDTO(
                log.getId(),
                log.getServiceOrder().getId(),
                log.getPreviousStatus(),
                log.getNewStatus(),
                log.getChangedBy(),
                log.getNote(),
                log.getCreatedAt()
        );
    }
}
