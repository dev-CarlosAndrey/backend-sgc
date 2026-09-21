package br.com.fpo.sgc.domain.serviceorder.statuslog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ServiceOrderStatusLogRepository extends JpaRepository<ServiceOrderStatusLog, Long> {
    Page<ServiceOrderStatusLog> findByServiceOrderIdAndActiveTrue(Long serviceOrderId, Pageable pageable);

    // Histórico das OS abertas no período (ordem cronológica por OS), para medir o tempo em cada etapa.
    @Query("SELECT l FROM ServiceOrderStatusLog l JOIN FETCH l.serviceOrder so " +
            "WHERE l.active = true AND so.active = true " +
            "AND (:openedFrom IS NULL OR so.createdAt >= :openedFrom) AND (:openedTo IS NULL OR so.createdAt < :openedTo) " +
            "AND (:companyId IS NULL OR so.sector.building.company.id = :companyId) " +
            "ORDER BY so.id, l.createdAt, l.id")
    List<ServiceOrderStatusLog> findForReport(@Param("openedFrom") Instant openedFrom, @Param("openedTo") Instant openedTo,
                                              @Param("companyId") Long companyId);
}
