package br.com.fpo.sgc.domain.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findByEntityNameAndEntityId(String entityName, Long entityId, Pageable pageable);
    Page<AuditLog> findByEntityName(String entityName, Pageable pageable);

    String FILTER = "WHERE (:from IS NULL OR a.createdAt >= :from) AND (:to IS NULL OR a.createdAt < :to) " +
            "AND (:entityName IS NULL OR a.entityName = :entityName) " +
            "AND (:performedBy IS NULL OR a.performedBy = :performedBy) ";

    @Query("SELECT a.entityName, COUNT(a) FROM AuditLog a " + FILTER + "GROUP BY a.entityName ORDER BY COUNT(a) DESC")
    List<Object[]> countByEntity(@Param("from") Instant from, @Param("to") Instant to,
                                 @Param("entityName") String entityName, @Param("performedBy") String performedBy);

    @Query("SELECT a.action, COUNT(a) FROM AuditLog a " + FILTER + "GROUP BY a.action ORDER BY COUNT(a) DESC")
    List<Object[]> countByAction(@Param("from") Instant from, @Param("to") Instant to,
                                 @Param("entityName") String entityName, @Param("performedBy") String performedBy);

    @Query("SELECT a.performedBy, COUNT(a) FROM AuditLog a " + FILTER + "GROUP BY a.performedBy ORDER BY COUNT(a) DESC")
    List<Object[]> countByUser(@Param("from") Instant from, @Param("to") Instant to,
                               @Param("entityName") String entityName, @Param("performedBy") String performedBy);

    @Query("SELECT a FROM AuditLog a " + FILTER + "ORDER BY a.createdAt DESC, a.id DESC")
    List<AuditLog> findForReport(@Param("from") Instant from, @Param("to") Instant to,
                                 @Param("entityName") String entityName, @Param("performedBy") String performedBy,
                                 Pageable pageable);
}
