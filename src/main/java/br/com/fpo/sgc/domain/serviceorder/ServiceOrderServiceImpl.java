package br.com.fpo.sgc.domain.serviceorder;

import br.com.fpo.sgc.common.exception.BusinessException;
import br.com.fpo.sgc.common.util.PeriodRange;
import br.com.fpo.sgc.domain.asset.Asset;
import br.com.fpo.sgc.domain.asset.AssetRepository;
import br.com.fpo.sgc.domain.asset.AssetStatus;
import br.com.fpo.sgc.domain.audit.AuditLogService;
import br.com.fpo.sgc.domain.location.sector.Sector;
import br.com.fpo.sgc.domain.location.sector.SectorRepository;
import br.com.fpo.sgc.domain.serviceorder.dto.ServiceOrderRequestDTO;
import br.com.fpo.sgc.domain.serviceorder.dto.ServiceOrderResponseDTO;
import br.com.fpo.sgc.domain.serviceorder.dto.ServiceOrderStatusChangeRequestDTO;
import br.com.fpo.sgc.domain.serviceorder.mapper.ServiceOrderMapper;
import br.com.fpo.sgc.domain.serviceorder.statuslog.ServiceOrderStatusLog;
import br.com.fpo.sgc.domain.serviceorder.statuslog.ServiceOrderStatusLogRepository;
import br.com.fpo.sgc.domain.serviceorder.statuslog.dto.ServiceOrderStatusLogResponseDTO;
import br.com.fpo.sgc.domain.user.User;
import br.com.fpo.sgc.domain.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ServiceOrderServiceImpl implements ServiceOrderService {

    private final ServiceOrderRepository serviceOrderRepository;
    private final UserRepository userRepository;
    private final SectorRepository sectorRepository;
    private final AssetRepository assetRepository;
    private final ServiceOrderStatusLogRepository statusLogRepository;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;
    private final ServiceOrderMapper serviceOrderMapper;

    @Override
    @Transactional
    public ServiceOrderResponseDTO create(ServiceOrderRequestDTO dto) {
        // @Transactional aqui também (e não só no overload abaixo) porque esta chamada a create(dto, channel)
        // é auto-invocação (mesmo bean): sem isso, o proxy do Spring não entraria em ação e a transação não
        // seria aberta. Como os dois usam propagação REQUIRED (padrão), abrir aqui já cobre o método interno.
        return create(dto, ServiceOrderChannel.PANEL);
    }

    @Override
    @Transactional
    public ServiceOrderResponseDTO create(ServiceOrderRequestDTO dto, ServiceOrderChannel channel) {
        User requester = userRepository.findById(dto.requesterId())
                .orElseThrow(() -> new EntityNotFoundException("Solicitante não encontrado com ID: " + dto.requesterId()));

        Sector sector = sectorRepository.findById(dto.sectorId())
                .orElseThrow(() -> new EntityNotFoundException("Setor não encontrado com ID: " + dto.sectorId()));

        Asset asset = null;
        if (dto.assetId() != null) {
            asset = assetRepository.findById(dto.assetId())
                    .orElseThrow(() -> new EntityNotFoundException("Ativo não encontrado com ID: " + dto.assetId()));

            asset.setStatus(AssetStatus.IN_MAINTENANCE);
            assetRepository.save(asset);
        }

        ServiceOrder so = serviceOrderMapper.toEntity(dto, requester, sector, asset);
        so.setStatus(ServiceOrderStatus.OPEN);
        so.setChannel(channel);

        ServiceOrder savedSo = serviceOrderRepository.save(so);
        logStatusChange(savedSo, null, ServiceOrderStatus.OPEN, "Ordem de serviço aberta");
        publishEvent(savedSo, ServiceOrderEventType.CREATED);

        // Registrar auditoria
        auditLogService.log(
                "ServiceOrder",
                savedSo.getId(),
                "CREATE",
                requester.getEmail(),
                "Ordem de serviço criada: " + savedSo.getTitle()
        );

        return serviceOrderMapper.toDTO(savedSo);
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceOrderResponseDTO findById(Long id) {
        ServiceOrder so = serviceOrderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ordem de serviço não encontrada com ID: " + id));
        return serviceOrderMapper.toDTO(so);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ServiceOrderResponseDTO> findAll(Pageable pageable) {
        return serviceOrderRepository.findByActiveTrue(pageable).map(serviceOrderMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ServiceOrderResponseDTO> findByStatus(ServiceOrderStatus status, Pageable pageable) {
        return serviceOrderRepository.findByStatusAndActiveTrue(status, pageable).map(serviceOrderMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ServiceOrderResponseDTO> findByAssetId(Long assetId, Pageable pageable) {
        return serviceOrderRepository.findByAssetIdAndActiveTrue(assetId, pageable).map(serviceOrderMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ServiceOrderResponseDTO> findByRequesterId(Long requesterId, Pageable pageable) {
        return serviceOrderRepository.findByRequesterIdAndActiveTrue(requesterId, pageable).map(serviceOrderMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ServiceOrderResponseDTO> findByTechnicianId(Long technicianId, Pageable pageable) {
        return serviceOrderRepository.findByTechnicianIdAndActiveTrue(technicianId, pageable).map(serviceOrderMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ServiceOrderResponseDTO> findByPeriod(LocalDate from, LocalDate to, ServiceOrderPeriodField field, Pageable pageable) {
        if (from == null && to == null) {
            throw new BusinessException("Informe ao menos uma das datas do período (from ou to).");
        }
        PeriodRange range = PeriodRange.of(from, to);

        Page<ServiceOrder> page = field == ServiceOrderPeriodField.CLOSED
                ? serviceOrderRepository.findByClosedBetween(range.from(), range.to(), pageable)
                : serviceOrderRepository.findByOpenedBetween(range.from(), range.to(), pageable);
        return page.map(serviceOrderMapper::toDTO);
    }

    @Override
    @Transactional
    public ServiceOrderResponseDTO assignTechnician(Long id, Long technicianId) {
        ServiceOrder so = serviceOrderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ordem de serviço não encontrada com ID: " + id));

        User technician = userRepository.findById(technicianId)
                .orElseThrow(() -> new EntityNotFoundException("Técnico não encontrado com ID: " + technicianId));

        ServiceOrderStatus previousStatus = so.getStatus();
        so.setTechnician(technician);
        so.setStatus(ServiceOrderStatus.IN_PROGRESS);

        ServiceOrder updatedSo = serviceOrderRepository.save(so);
        logStatusChange(updatedSo, previousStatus, ServiceOrderStatus.IN_PROGRESS,
                "Técnico " + technician.getName() + " atribuído");
        publishEvent(updatedSo, ServiceOrderEventType.TECHNICIAN_ASSIGNED);

        // Registrar auditoria
        auditLogService.log(
                "ServiceOrder",
                updatedSo.getId(),
                "ASSIGN_TECHNICIAN",
                technician.getEmail(),
                "Técnico " + technician.getName() + " atribuído ao chamado"
        );

        return serviceOrderMapper.toDTO(updatedSo);
    }

    @Override
    @Transactional
    public ServiceOrderResponseDTO resolveOrder(Long id, String solutionDetails, BigDecimal cost) {
        ServiceOrder so = serviceOrderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ordem de serviço não encontrada com ID: " + id));

        if (so.getStatus() == ServiceOrderStatus.RESOLVED) {
            throw new BusinessException("Esta ordem de serviço já se encontra finalizada.");
        }
        if (so.getStatus() == ServiceOrderStatus.CANCELLED) {
            throw new BusinessException("Não é possível resolver uma ordem de serviço cancelada.");
        }

        ServiceOrderStatus previousStatus = so.getStatus();
        so.setSolutionDetails(solutionDetails);
        so.setStatus(ServiceOrderStatus.RESOLVED);
        so.setClosedAt(Instant.now());
        so.setCost(cost);

        if (so.getAsset() != null) {
            so.getAsset().setStatus(AssetStatus.IN_USE);
            assetRepository.save(so.getAsset());
        }

        ServiceOrder resolvedSo = serviceOrderRepository.save(so);
        logStatusChange(resolvedSo, previousStatus, ServiceOrderStatus.RESOLVED, solutionDetails);
        publishEvent(resolvedSo, ServiceOrderEventType.RESOLVED, currentUserId());

        // Registrar auditoria
        String performedBy = (resolvedSo.getTechnician() != null) ? resolvedSo.getTechnician().getEmail() : "SYSTEM";
        auditLogService.log(
                "ServiceOrder",
                resolvedSo.getId(),
                "RESOLVE",
                performedBy,
                "Ordem de serviço resolvida. Solução: " + solutionDetails
        );

        return serviceOrderMapper.toDTO(resolvedSo);
    }

    @Override
    @Transactional
    public ServiceOrderResponseDTO changeStatus(Long id, ServiceOrderStatusChangeRequestDTO dto) {
        ServiceOrder so = serviceOrderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ordem de serviço não encontrada com ID: " + id));

        ServiceOrderStatus previousStatus = so.getStatus();
        ServiceOrderStatus newStatus = dto.status();

        if (previousStatus == ServiceOrderStatus.RESOLVED || previousStatus == ServiceOrderStatus.CANCELLED) {
            throw new BusinessException("Não é possível alterar o status de uma ordem de serviço já finalizada ou cancelada.");
        }
        if (newStatus == previousStatus) {
            throw new BusinessException("A ordem de serviço já se encontra no status " + newStatus + ".");
        }
        if (newStatus == ServiceOrderStatus.RESOLVED) {
            throw new BusinessException("Para resolver a ordem de serviço, utilize o endpoint de resolução (com laudo técnico).");
        }
        if (newStatus == ServiceOrderStatus.OPEN) {
            throw new BusinessException("Não é possível reabrir uma ordem de serviço em andamento.");
        }
        if (newStatus == ServiceOrderStatus.IN_PROGRESS && so.getTechnician() == null) {
            throw new BusinessException("Atribua um técnico antes de colocar a ordem de serviço em andamento.");
        }

        so.setStatus(newStatus);

        if (newStatus == ServiceOrderStatus.CANCELLED) {
            so.setClosedAt(Instant.now());
            if (so.getAsset() != null) {
                so.getAsset().setStatus(AssetStatus.IN_USE);
                assetRepository.save(so.getAsset());
            }
        }

        ServiceOrder updatedSo = serviceOrderRepository.save(so);
        logStatusChange(updatedSo, previousStatus, newStatus, dto.note());
        publishEvent(updatedSo, ServiceOrderEventType.STATUS_CHANGED);

        auditLogService.log(
                "ServiceOrder",
                updatedSo.getId(),
                "CHANGE_STATUS",
                currentUser(),
                "Status alterado de " + previousStatus + " para " + newStatus
        );

        return serviceOrderMapper.toDTO(updatedSo);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ServiceOrderStatusLogResponseDTO> findStatusHistory(Long id, Pageable pageable) {
        if (!serviceOrderRepository.existsById(id)) {
            throw new EntityNotFoundException("Ordem de serviço não encontrada com ID: " + id);
        }
        return statusLogRepository.findByServiceOrderIdAndActiveTrue(id, pageable)
                .map(ServiceOrderStatusLogResponseDTO::fromEntity);
    }

    // Consumido após o commit (ex.: notificações); se a transação sofrer rollback, o evento é descartado.
    private void publishEvent(ServiceOrder so, ServiceOrderEventType type) {
        publishEvent(so, type, null);
    }

    private void publishEvent(ServiceOrder so, ServiceOrderEventType type, Long actorUserId) {
        eventPublisher.publishEvent(new ServiceOrderEvent(so.getId(), type, so.getStatus(), actorUserId));
    }

    // Id do usuário autenticado (o login é o name do usuário); nulo se não houver usuário logado.
    private Long currentUserId() {
        String username = currentUser();
        return userRepository.findByNameAndActiveTrue(username).map(User::getId).orElse(null);
    }

    private void logStatusChange(ServiceOrder so, ServiceOrderStatus previous, ServiceOrderStatus next, String note) {
        statusLogRepository.save(ServiceOrderStatusLog.builder()
                .serviceOrder(so)
                .previousStatus(previous)
                .newStatus(next)
                .changedBy(currentUser())
                .note(note)
                .build());
    }

    // Usuário autenticado na requisição; "SYSTEM" quando não há (mesmo critério do AuditLogServiceImpl).
    private String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return "SYSTEM";
        }
        return auth.getName();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ServiceOrder so = serviceOrderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ordem de serviço não encontrada com ID: " + id));

        so.setActive(false);
        serviceOrderRepository.save(so);

        // Registrar auditoria
        auditLogService.log(
                "ServiceOrder",
                id,
                "DELETE",
                "ADMIN",
                "Ordem de serviço inativada/excluída historicamente"
        );
    }
}
