package br.com.fpo.sgc.domain.whatsapp;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WhatsAppInboundMessageRepository extends JpaRepository<WhatsAppInboundMessage, Long> {
    boolean existsByMessageId(String messageId);
}
