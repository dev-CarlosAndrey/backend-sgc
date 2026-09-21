package br.com.fpo.sgc.domain.serviceorder;

import br.com.fpo.sgc.common.exception.BusinessException;
import br.com.fpo.sgc.domain.asset.Asset;
import br.com.fpo.sgc.domain.asset.AssetRepository;
import br.com.fpo.sgc.domain.asset.AssetStatus;
import br.com.fpo.sgc.domain.audit.AuditLogService;
import br.com.fpo.sgc.domain.location.sector.Sector;
import br.com.fpo.sgc.domain.location.sector.SectorRepository;
import br.com.fpo.sgc.domain.serviceorder.dto.ServiceOrderRequestDTO;
import br.com.fpo.sgc.domain.serviceorder.dto.ServiceOrderResponseDTO;
import br.com.fpo.sgc.domain.serviceorder.mapper.ServiceOrderMapper;
import br.com.fpo.sgc.domain.user.User;
import br.com.fpo.sgc.domain.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ServiceOrderServiceImpl implements ServiceOrderService {

    private final ServiceOrderRepository serviceOrderRepository;
    private final UserRepository userRepository;
    private final SectorRepository sectorRepository;
    private final AssetRepository assetRepository;
    private final AuditLogService auditLogService;
    private final ServiceOrderMapper serviceOrderMapper;

    @Override
    @Transactional
    public ServiceOrderResponseDTO create(ServiceOrderRequestDTO dto) {
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
        so.setChannel(ServiceOrderChannel.PANEL);

        ServiceOrder savedSo = serviceOrderRepository.save(so);

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
    @Transactional
    public ServiceOrderResponseDTO assignTechnician(Long id, Long technicianId) {
        ServiceOrder so = serviceOrderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ordem de serviço não encontrada com ID: " + id));

        User technician = userRepository.findById(technicianId)
                .orElseThrow(() -> new EntityNotFoundException("Técnico não encontrado com ID: " + technicianId));

        so.setTechnician(technician);
        so.setStatus(ServiceOrderStatus.IN_PROGRESS);

        ServiceOrder updatedSo = serviceOrderRepository.save(so);

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

        so.setSolutionDetails(solutionDetails);
        so.setStatus(ServiceOrderStatus.RESOLVED);
        so.setClosedAt(Instant.now());
        so.setCost(cost);

        if (so.getAsset() != null) {
            so.getAsset().setStatus(AssetStatus.IN_USE);
            assetRepository.save(so.getAsset());
        }

        ServiceOrder resolvedSo = serviceOrderRepository.save(so);

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
