package br.com.fpo.sgc.domain.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByServiceOrderIdAndActiveTrue(Long serviceOrderId, Pageable pageable);

    // Base do relatório de notificações: tentativas no período, ordenadas para que as tentativas de uma
    // mesma notificação (OS + destinatário, em sequência) fiquem contíguas.
    @Query("SELECT n FROM Notification n WHERE n.active = true " +
            "AND (:from IS NULL OR n.createdAt >= :from) AND (:to IS NULL OR n.createdAt < :to) " +
            "AND (:companyId IS NULL OR n.serviceOrder.sector.building.company.id = :companyId) " +
            "ORDER BY n.serviceOrder.id, n.recipient.id, n.createdAt, n.id")
    List<Notification> findForReport(@Param("from") Instant from, @Param("to") Instant to,
                                     @Param("companyId") Long companyId);
}
