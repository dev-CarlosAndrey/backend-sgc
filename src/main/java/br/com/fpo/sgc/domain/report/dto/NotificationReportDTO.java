package br.com.fpo.sgc.domain.report.dto;

import java.util.List;

/**
 * Entrega das notificações de OS no período.
 * - totalAttempts: linhas de tb_notifications (cada tentativa em cada canal).
 * - totalNotifications: notificações (um destinatário em um evento da OS), com todas as suas tentativas.
 * - deliveryRatePercent: notificações entregues por algum canal / total.
 * - fallbackRatePercent: das notificações que tentaram WhatsApp, quantas precisaram cair para o e-mail.
 * - byOutcome: WHATSAPP_FIRST_ATTEMPT, WHATSAPP_AFTER_RETRY, EMAIL_FALLBACK, EMAIL_ONLY, NOT_DELIVERED.
 * - topErrors: até 5 mensagens de erro mais frequentes.
 */
public record NotificationReportDTO(long totalAttempts, long totalNotifications, long delivered,
                                    Double deliveryRatePercent, Double fallbackRatePercent,
                                    List<NotificationChannelRowDTO> channels,
                                    List<LabelCountDTO> byOutcome, List<LabelCountDTO> topErrors) {
}
