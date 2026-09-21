package br.com.fpo.sgc.domain.serviceorder;

import br.com.fpo.sgc.domain.serviceorder.dto.ServiceOrderRequestDTO;
import br.com.fpo.sgc.domain.serviceorder.dto.ServiceOrderResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ServiceOrderService {
    ServiceOrderResponseDTO create(ServiceOrderRequestDTO dto);
    ServiceOrderResponseDTO findById(Long id);
    Page<ServiceOrderResponseDTO> findAll(Pageable pageable);
    Page<ServiceOrderResponseDTO> findByStatus(ServiceOrderStatus status, Pageable pageable);
    Page<ServiceOrderResponseDTO> findByAssetId(Long assetId, Pageable pageable);
    Page<ServiceOrderResponseDTO> findByRequesterId(Long requesterId, Pageable pageable);
    Page<ServiceOrderResponseDTO> findByTechnicianId(Long technicianId, Pageable pageable);
    ServiceOrderResponseDTO assignTechnician(Long id, Long technicianId);
    ServiceOrderResponseDTO resolveOrder(Long id, String solutionDetails);
    void delete(Long id);
}
