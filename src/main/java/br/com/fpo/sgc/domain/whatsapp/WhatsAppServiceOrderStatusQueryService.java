package br.com.fpo.sgc.domain.whatsapp;

import br.com.fpo.sgc.domain.serviceorder.ServiceOrderService;
import br.com.fpo.sgc.domain.serviceorder.dto.ServiceOrderResponseDTO;
import br.com.fpo.sgc.domain.user.User;
import br.com.fpo.sgc.domain.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Responde a uma consulta de status (RF16). Ao contrário da abertura de OS (RF15), aqui NÃO criamos um
 * {@link User} na hora: quem nunca abriu chamado não tem nada para consultar, então um telefone
 * desconhecido simplesmente recebe "sem histórico" em vez de ganhar um cadastro.
 *
 * Propositalmente SEM {@code @Transactional} própria: {@code serviceOrderService.findById} já é
 * transacional e, ao lançar {@code EntityNotFoundException} (bem) quando a OS não existe, marcaria como
 * "rollback-only" qualquer transação externa que a tivesse chamado como participante — mesmo capturando a
 * exceção aqui, o commit dessa transação falharia depois com {@code UnexpectedRollbackException}. Cada
 * chamada abaixo gerencia sua própria transação, então esse problema não se aplica.
 */
@Service
@RequiredArgsConstructor
class WhatsAppServiceOrderStatusQueryService {

    private static final int RECENT_ORDERS_LIMIT = 5;

    private final UserRepository userRepository;
    private final ServiceOrderService serviceOrderService;

    StatusQueryResult query(WhatsAppServiceOrderStatusQueryCommand command, String phone) {
        Optional<User> requester = userRepository.findByPhone(phone);
        if (requester.isEmpty()) {
            return StatusQueryResult.noHistory();
        }

        if (command.serviceOrderId() != null) {
            return querySingle(command.serviceOrderId(), requester.get());
        }
        return queryRecent(requester.get());
    }

    // Não distingue "não existe" de "não é seu" na resposta: evita confirmar para quem pergunta se um
    // número de chamado de outra pessoa existe.
    private StatusQueryResult querySingle(Long serviceOrderId, User requester) {
        ServiceOrderResponseDTO serviceOrder;
        try {
            serviceOrder = serviceOrderService.findById(serviceOrderId);
        } catch (EntityNotFoundException e) {
            return StatusQueryResult.notFound(serviceOrderId);
        }

        if (!requester.getId().equals(serviceOrder.requester().id())) {
            return StatusQueryResult.notFound(serviceOrderId);
        }
        return StatusQueryResult.single(serviceOrder);
    }

    private StatusQueryResult queryRecent(User requester) {
        List<ServiceOrderResponseDTO> recent = serviceOrderService
                .findByRequesterId(requester.getId(), PageRequest.of(0, RECENT_ORDERS_LIMIT, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent();

        return recent.isEmpty() ? StatusQueryResult.noHistory() : StatusQueryResult.list(recent);
    }

    sealed interface StatusQueryResult {
        record NoHistory() implements StatusQueryResult {}
        record NotFound(Long serviceOrderId) implements StatusQueryResult {}
        record Single(ServiceOrderResponseDTO serviceOrder) implements StatusQueryResult {}
        record RecentList(List<ServiceOrderResponseDTO> serviceOrders) implements StatusQueryResult {}

        static StatusQueryResult noHistory() {
            return new NoHistory();
        }

        static StatusQueryResult notFound(Long serviceOrderId) {
            return new NotFound(serviceOrderId);
        }

        static StatusQueryResult single(ServiceOrderResponseDTO serviceOrder) {
            return new Single(serviceOrder);
        }

        static StatusQueryResult list(List<ServiceOrderResponseDTO> serviceOrders) {
            return new RecentList(serviceOrders);
        }
    }
}
