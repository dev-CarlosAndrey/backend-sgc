package br.com.fpo.sgc.domain.serviceorder.statuslog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceOrderStatusLogRepository extends JpaRepository<ServiceOrderStatusLog, Long> {
    Page<ServiceOrderStatusLog> findByServiceOrderIdAndActiveTrue(Long serviceOrderId, Pageable pageable);
}
