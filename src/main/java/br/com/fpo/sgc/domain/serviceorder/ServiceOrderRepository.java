package br.com.fpo.sgc.domain.serviceorder;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceOrderRepository extends JpaRepository<ServiceOrder, Long> {
    Page<ServiceOrder> findByActiveTrue(Pageable pageable);
    Page<ServiceOrder> findByStatusAndActiveTrue(ServiceOrderStatus status, Pageable pageable);
    Page<ServiceOrder> findByRequesterIdAndActiveTrue(Long requesterId, Pageable pageable);
    Page<ServiceOrder> findByTechnicianIdAndActiveTrue(Long technicianId, Pageable pageable);
    Page<ServiceOrder> findByAssetIdAndActiveTrue(Long assetId, Pageable pageable);
}
