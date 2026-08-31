package br.com.fpo.sgc.domain.audit.dto;

import br.com.fpo.sgc.domain.audit.AuditLog;

import java.time.Instant;

public record AuditLogResponseDTO(
        Long id,
        String entityName,
        Long entityId,
        String action,
        String performedBy,
        String details,
        Instant createdAt
) {
    public static AuditLogResponseDTO fromEntity(AuditLog log) {
        return new AuditLogResponseDTO(
                log.getId(),
                log.getEntityName(),
                log.getEntityId(),
                log.getAction(),
                log.getPerformedBy(),
                log.getDetails(),
                log.getCreatedAt()
        );
    }
}