package br.com.fpo.sgc.domain.audit;

import br.com.fpo.sgc.domain.audit.dto.AuditLogResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional
    public void log(String entityName, Long entityId, String action, String performedBy, String details) {
        AuditLog auditLog = AuditLog.builder()
                .entityName(entityName)
                .entityId(entityId)
                .action(action)
                .performedBy(performedBy != null ? performedBy : "SYSTEM")
                .details(details)
                .build();

        auditLogRepository.save(auditLog);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponseDTO> findByEntity(String entityName, Long entityId, Pageable pageable) {
        return auditLogRepository.findByEntityNameAndEntityId(entityName, entityId, pageable)
                .map(AuditLogResponseDTO::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponseDTO> findAll(Pageable pageable) {
        return auditLogRepository.findAll(pageable).map(AuditLogResponseDTO::fromEntity);
    }
}