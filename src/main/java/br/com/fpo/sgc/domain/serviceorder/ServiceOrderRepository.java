package br.com.fpo.sgc.domain.serviceorder;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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

    // Base dos relatórios: filtros opcionais (null = sem filtro) e tudo o que o relatório exibe já carregado.
    @Query("SELECT so FROM ServiceOrder so JOIN FETCH so.requester JOIN FETCH so.sector " +
            "LEFT JOIN FETCH so.technician LEFT JOIN FETCH so.asset " +
            "WHERE so.active = true " +
            "AND (:openedFrom IS NULL OR so.createdAt >= :openedFrom) AND (:openedTo IS NULL OR so.createdAt < :openedTo) " +
            "AND (:closedFrom IS NULL OR so.closedAt >= :closedFrom) AND (:closedTo IS NULL OR so.closedAt < :closedTo) " +
            "AND (:technicianId IS NULL OR so.technician.id = :technicianId) " +
            "AND (:status IS NULL OR so.status = :status) " +
            "AND (:companyId IS NULL OR so.sector.building.company.id = :companyId) " +
            "ORDER BY so.createdAt")
    List<ServiceOrder> findForReport(@Param("openedFrom") Instant openedFrom, @Param("openedTo") Instant openedTo,
                                     @Param("closedFrom") Instant closedFrom, @Param("closedTo") Instant closedTo,
                                     @Param("technicianId") Long technicianId,
                                     @Param("status") ServiceOrderStatus status,
                                     @Param("companyId") Long companyId);

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

    // Listagem com busca e filtros opcionais (null = sem filtro). `q` já chega como padrão LIKE
    // montado por SearchTerm.toLikePattern. `requesterId` restringe ao solicitante (perfil USER).
    @Query("SELECT so FROM ServiceOrder so JOIN so.requester r JOIN so.sector s " +
            "WHERE so.active = true " +
            "AND (:status IS NULL OR so.status = :status) " +
            "AND (:channel IS NULL OR so.channel = :channel) " +
            "AND (:requesterId IS NULL OR r.id = :requesterId) " +
            "AND (:q IS NULL " +
            "OR LOWER(so.title) LIKE :q ESCAPE '!' " +
            "OR LOWER(r.name) LIKE :q ESCAPE '!' " +
            "OR LOWER(s.name) LIKE :q ESCAPE '!')")
    Page<ServiceOrder> search(@Param("q") String q,
                              @Param("status") ServiceOrderStatus status,
                              @Param("channel") ServiceOrderChannel channel,
                              @Param("requesterId") Long requesterId,
                              Pageable pageable);
}
