package br.com.fpo.sgc.domain.serviceorder;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@Repository
public interface ServiceOrderRepository extends JpaRepository<ServiceOrder, Long> {
    Page<ServiceOrder> findByActiveTrue(Pageable pageable);
    Page<ServiceOrder> findByStatusAndActiveTrue(ServiceOrderStatus status, Pageable pageable);
    Page<ServiceOrder> findByRequesterIdAndActiveTrue(Long requesterId, Pageable pageable);
    Page<ServiceOrder> findByTechnicianIdAndActiveTrue(Long technicianId, Pageable pageable);
    Page<ServiceOrder> findByAssetIdAndActiveTrue(Long assetId, Pageable pageable);

    @Query("SELECT so FROM ServiceOrder so WHERE so.active = true " +
            "AND (:from IS NULL OR so.createdAt >= :from) AND (:to IS NULL OR so.createdAt < :to)")
    Page<ServiceOrder> findByOpenedBetween(@Param("from") Instant from, @Param("to") Instant to, Pageable pageable);

    @Query("SELECT so FROM ServiceOrder so WHERE so.active = true " +
            "AND (:from IS NULL OR so.closedAt >= :from) AND (:to IS NULL OR so.closedAt < :to)")
    Page<ServiceOrder> findByClosedBetween(@Param("from") Instant from, @Param("to") Instant to, Pageable pageable);

    // Carrega solicitante e técnico junto, para uso fora de transação (ex.: listener assíncrono de notificações).
    @Query("SELECT so FROM ServiceOrder so JOIN FETCH so.requester LEFT JOIN FETCH so.technician WHERE so.id = :id")
    Optional<ServiceOrder> findWithParticipantsById(@Param("id") Long id);

    @Query("SELECT COUNT(so) FROM ServiceOrder so " +
            "WHERE so.active = true " +
            "AND so.status NOT IN (br.com.fpo.sgc.domain.serviceorder.ServiceOrderStatus.RESOLVED, br.com.fpo.sgc.domain.serviceorder.ServiceOrderStatus.CANCELLED) " +
            "AND (:companyId IS NULL OR so.sector.building.company.id = :companyId)")
    long countActiveByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT COALESCE(SUM(so.cost), 0) FROM ServiceOrder so " +
            "WHERE so.active = true AND so.cost IS NOT NULL " +
            "AND (:companyId IS NULL OR so.sector.building.company.id = :companyId)")
    BigDecimal sumMaintenanceCostByCompanyId(@Param("companyId") Long companyId);
}
