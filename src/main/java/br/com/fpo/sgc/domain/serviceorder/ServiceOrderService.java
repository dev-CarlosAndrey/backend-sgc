package br.com.fpo.sgc.domain.serviceorder;

import br.com.fpo.sgc.domain.serviceorder.dto.ServiceOrderRequestDTO;
import br.com.fpo.sgc.domain.serviceorder.dto.ServiceOrderResponseDTO;
import br.com.fpo.sgc.domain.serviceorder.dto.ServiceOrderStatusChangeRequestDTO;
import br.com.fpo.sgc.domain.serviceorder.statuslog.dto.ServiceOrderStatusLogResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface ServiceOrderService {
    ServiceOrderResponseDTO create(ServiceOrderRequestDTO dto);
    // Usado pela abertura de OS via WhatsApp (RF15) para marcar channel = WHATSAPP; create(dto) acima
    // continua fixando PANEL, usado pelo painel administrativo.
    ServiceOrderResponseDTO create(ServiceOrderRequestDTO dto, ServiceOrderChannel channel);
    ServiceOrderResponseDTO findById(Long id);
    Page<ServiceOrderResponseDTO> findAll(String q, ServiceOrderStatus status, ServiceOrderChannel channel, Pageable pageable);
    Page<ServiceOrderResponseDTO> findByStatus(ServiceOrderStatus status, Pageable pageable);
    Page<ServiceOrderResponseDTO> findByAssetId(Long assetId, Pageable pageable);
    Page<ServiceOrderResponseDTO> findByRequesterId(Long requesterId, Pageable pageable);
    Page<ServiceOrderResponseDTO> findByTechnicianId(Long technicianId, Pageable pageable);
    Page<ServiceOrderResponseDTO> findByPeriod(LocalDate from, LocalDate to, ServiceOrderPeriodField field, Pageable pageable);
    ServiceOrderResponseDTO assignTechnician(Long id, Long technicianId);
    ServiceOrderResponseDTO resolveOrder(Long id, String solutionDetails, BigDecimal cost);
    ServiceOrderResponseDTO changeStatus(Long id, ServiceOrderStatusChangeRequestDTO dto);
    Page<ServiceOrderStatusLogResponseDTO> findStatusHistory(Long id, Pageable pageable);
    void delete(Long id);
}
