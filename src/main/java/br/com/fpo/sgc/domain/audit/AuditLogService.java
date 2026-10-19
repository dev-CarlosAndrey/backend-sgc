package br.com.fpo.sgc.domain.audit;

import br.com.fpo.sgc.domain.audit.dto.AuditLogResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditLogService {
    void log(String entityName, Long entityId, String action, String performedBy, String details);
    Page<AuditLogResponseDTO> findByEntity(String entityName, Long entityId, Pageable pageable);
    Page<AuditLogResponseDTO> findAll(String q, Pageable pageable);
}