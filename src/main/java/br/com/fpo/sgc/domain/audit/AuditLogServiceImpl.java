package br.com.fpo.sgc.domain.audit;

import br.com.fpo.sgc.domain.audit.dto.AuditLogResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
                .performedBy(currentUser())
                .details(details)
                .build();

        auditLogRepository.save(auditLog);
    }

    // Responsável = usuário autenticado na requisição; "SYSTEM" quando não há usuário logado.
    // O parâmetro performedBy de log(...) é ignorado (os services passavam o nome do registro afetado).
    private String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return "SYSTEM";
        }
        return auth.getName();
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