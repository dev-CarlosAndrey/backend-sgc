package br.com.fpo.sgc.domain.serviceorder;

/**
 * Evento de ciclo de vida da OS, publicado pelo service e consumido após o commit (ex.: notificações).
 * actorUserId = usuário que executou a ação, quando relevante para quem deve ser avisado (ex.: quem
 * resolveu a OS); nulo nos demais eventos.
 */
public record ServiceOrderEvent(Long serviceOrderId, ServiceOrderEventType type, ServiceOrderStatus status,
                                Long actorUserId) {

    public ServiceOrderEvent(Long serviceOrderId, ServiceOrderEventType type, ServiceOrderStatus status) {
        this(serviceOrderId, type, status, null);
    }
}
