package br.com.fpo.sgc.domain.whatsapp;

import br.com.fpo.sgc.domain.notification.WhatsAppNotificationSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Reage a toda mensagem de texto recebida no número comercial do WhatsApp (RF14/RF15/RF16): se o texto
 * estiver no formato de abertura de OS ({@link WhatsAppServiceOrderCommand}), delega a criação do chamado
 * para {@link WhatsAppServiceOrderIntakeService}; se for um comando de consulta de status
 * ({@link WhatsAppServiceOrderStatusQueryCommand}), delega para
 * {@link WhatsAppServiceOrderStatusQueryService}; se for uma avaliação ({@link WhatsAppServiceOrderRatingCommand}),
 * para {@link WhatsAppServiceOrderRatingService}; caso contrário, responde com a mensagem de boas-vindas.
 * Roda fora da requisição do webhook ({@code @Async}, ver {@code AsyncConfig}) para não atrasar a resposta
 * que a Meta espera; falha de envio ou de processamento nunca deve propagar para o webhook.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppAutoReplyService {

    private final WhatsAppNotificationSender whatsAppNotificationSender;
    private final WhatsAppServiceOrderIntakeService intakeService;
    private final WhatsAppServiceOrderStatusQueryService statusQueryService;
    private final WhatsAppServiceOrderRatingService ratingService;

    @Async
    public void handleIncomingMessage(String fromPhoneNumber, String displayName, String text) {
        if (!whatsAppNotificationSender.isConfigured()) {
            return;
        }
        try {
            String reply = WhatsAppServiceOrderCommand.parse(text)
                    .<String>map(command -> replyFor(intakeService.open(command, fromPhoneNumber, displayName)))
                    .or(() -> WhatsAppServiceOrderStatusQueryCommand.parse(text)
                            .map(command -> replyFor(statusQueryService.query(command, fromPhoneNumber))))
                    .or(() -> WhatsAppServiceOrderRatingCommand.parse(text)
                            .map(command -> replyFor(ratingService.rate(command, fromPhoneNumber))))
                    .orElseGet(WhatsAppMessageTemplates::welcomeMessage);

            whatsAppNotificationSender.sendText(fromPhoneNumber, reply);
        } catch (Exception e) {
            log.warn("Falha ao processar/responder mensagem do WhatsApp de {}: {}", fromPhoneNumber, e.getMessage());
        }
    }

    private String replyFor(WhatsAppServiceOrderIntakeService.IntakeResult result) {
        return switch (result) {
            case WhatsAppServiceOrderIntakeService.IntakeResult.Success success ->
                    WhatsAppMessageTemplates.serviceOrderCreatedMessage(
                            success.serviceOrder().id(),
                            success.serviceOrder().sector().name(),
                            success.serviceOrder().asset() != null);
            case WhatsAppServiceOrderIntakeService.IntakeResult.SectorNotFound sectorNotFound ->
                    WhatsAppMessageTemplates.sectorNotFoundMessage(sectorNotFound.sectorName());
            case WhatsAppServiceOrderIntakeService.IntakeResult.AssetNotFound assetNotFound ->
                    WhatsAppMessageTemplates.assetNotFoundMessage(assetNotFound.tagNumber());
        };
    }

    private String replyFor(WhatsAppServiceOrderStatusQueryService.StatusQueryResult result) {
        return switch (result) {
            case WhatsAppServiceOrderStatusQueryService.StatusQueryResult.NoHistory ignored ->
                    WhatsAppMessageTemplates.noServiceOrderHistoryMessage();
            case WhatsAppServiceOrderStatusQueryService.StatusQueryResult.NotFound notFound ->
                    WhatsAppMessageTemplates.serviceOrderNotFoundForPhoneMessage(notFound.serviceOrderId());
            case WhatsAppServiceOrderStatusQueryService.StatusQueryResult.Single single ->
                    WhatsAppMessageTemplates.serviceOrderStatusMessage(single.serviceOrder());
            case WhatsAppServiceOrderStatusQueryService.StatusQueryResult.RecentList recentList ->
                    WhatsAppMessageTemplates.serviceOrderListMessage(recentList.serviceOrders());
        };
    }

    private String replyFor(WhatsAppServiceOrderRatingService.RatingResult result) {
        return switch (result) {
            case WhatsAppServiceOrderRatingService.RatingResult.InvalidFormat ignored ->
                    WhatsAppMessageTemplates.ratingFormatMessage();
            case WhatsAppServiceOrderRatingService.RatingResult.NotFound notFound ->
                    WhatsAppMessageTemplates.serviceOrderNotFoundForPhoneMessage(notFound.serviceOrderId());
            case WhatsAppServiceOrderRatingService.RatingResult.Rejected rejected ->
                    WhatsAppMessageTemplates.ratingRejectedMessage(rejected.reason());
            case WhatsAppServiceOrderRatingService.RatingResult.Success success ->
                    WhatsAppMessageTemplates.ratingRegisteredMessage(success.serviceOrderId(), success.score());
        };
    }
}
