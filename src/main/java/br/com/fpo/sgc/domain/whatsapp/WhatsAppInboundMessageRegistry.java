package br.com.fpo.sgc.domain.whatsapp;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Garante que cada mensagem recebida (id da Meta) seja processada uma única vez: a Meta reenvia o evento
 * quando não recebe 200 a tempo, e sem isso o reenvio abriria uma OS duplicada (RNF11).
 *
 * Não é @Transactional de propósito: o saveAndFlush roda na transação do próprio repositório, e a violação
 * da chave única (duas entregas simultâneas do mesmo evento) é capturada aqui sem marcar rollback em
 * transação alheia.
 */
@Service
@RequiredArgsConstructor
public class WhatsAppInboundMessageRegistry {

    private final WhatsAppInboundMessageRepository repository;

    /** @return true se a mensagem é nova e deve ser processada; false se já foi vista antes. */
    public boolean registerIfNew(String messageId, String fromPhone) {
        if (messageId == null || messageId.isBlank()) {
            return true; // sem id não há como deduplicar; processa normalmente
        }
        if (repository.existsByMessageId(messageId)) {
            return false;
        }
        try {
            repository.saveAndFlush(WhatsAppInboundMessage.builder()
                    .messageId(messageId)
                    .fromPhone(fromPhone)
                    .build());
            return true;
        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }
}
