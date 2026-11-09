package br.com.fpo.sgc.domain.whatsapp;

import br.com.fpo.sgc.common.exception.BusinessException;
import br.com.fpo.sgc.domain.serviceorder.ServiceOrderService;
import br.com.fpo.sgc.domain.user.User;
import br.com.fpo.sgc.domain.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Registra a avaliação de atendimento enviada pelo WhatsApp. O solicitante é identificado pelo telefone e só
 * avalia as próprias OS; as regras (OS concluída, uma avaliação só) ficam em ServiceOrderService.rate.
 *
 * Propositalmente SEM {@code @Transactional} própria, pelo mesmo motivo de
 * {@link WhatsAppServiceOrderStatusQueryService}: as exceções de serviceOrderService.rate marcariam uma
 * transação externa como rollback-only mesmo sendo capturadas aqui.
 */
@Service
@RequiredArgsConstructor
class WhatsAppServiceOrderRatingService {

    private final UserRepository userRepository;
    private final ServiceOrderService serviceOrderService;

    RatingResult rate(WhatsAppServiceOrderRatingCommand command, String phone) {
        if (!command.isValid()) {
            return new RatingResult.InvalidFormat();
        }
        Optional<User> requester = userRepository.findByPhone(phone);
        if (requester.isEmpty()) {
            return new RatingResult.NotFound(command.serviceOrderId());
        }
        try {
            serviceOrderService.rate(command.serviceOrderId(), requester.get().getId(), command.score());
            return new RatingResult.Success(command.serviceOrderId(), command.score());
        } catch (EntityNotFoundException e) {
            return new RatingResult.NotFound(command.serviceOrderId());
        } catch (BusinessException e) {
            return new RatingResult.Rejected(e.getMessage());
        }
    }

    sealed interface RatingResult {
        record InvalidFormat() implements RatingResult {}
        record NotFound(Long serviceOrderId) implements RatingResult {}
        record Rejected(String reason) implements RatingResult {}
        record Success(Long serviceOrderId, int score) implements RatingResult {}
    }
}
