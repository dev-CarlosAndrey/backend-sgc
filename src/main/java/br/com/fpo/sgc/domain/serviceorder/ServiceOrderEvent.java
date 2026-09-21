package br.com.fpo.sgc.domain.serviceorder;

/** Evento de ciclo de vida da OS, publicado pelo service e consumido após o commit (ex.: notificações). */
public record ServiceOrderEvent(Long serviceOrderId, ServiceOrderEventType type, ServiceOrderStatus status) {
}
