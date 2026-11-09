package br.com.fpo.sgc.domain.whatsapp;

import br.com.fpo.sgc.domain.location.sector.dto.SectorResponseDTO;
import br.com.fpo.sgc.domain.notification.WhatsAppNotificationSender;
import br.com.fpo.sgc.domain.serviceorder.dto.ServiceOrderResponseDTO;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class WhatsAppAutoReplyServiceTest {

    private static final String COMANDO_VALIDO = "Abrir OS\nSetor: TI\nProblema: Notebook não liga";

    private final WhatsAppNotificationSender sender = mock(WhatsAppNotificationSender.class);
    private final WhatsAppServiceOrderIntakeService intakeService = mock(WhatsAppServiceOrderIntakeService.class);
    private final WhatsAppServiceOrderStatusQueryService statusQueryService = mock(WhatsAppServiceOrderStatusQueryService.class);
    private final WhatsAppServiceOrderRatingService ratingService = mock(WhatsAppServiceOrderRatingService.class);
    private final WhatsAppAutoReplyService service = new WhatsAppAutoReplyService(sender, intakeService, statusQueryService, ratingService);

    @Test
    void mensagemForaDoFormato_respondeComBoasVindas() {
        when(sender.isConfigured()).thenReturn(true);

        service.handleIncomingMessage("558899999999", "Maria", "oi, bom dia");

        verify(sender).sendText(eq("558899999999"), contains("Abrir OS"));
        verifyNoInteractions(intakeService);
        verifyNoInteractions(statusQueryService);
    }

    @Test
    void comandoDeStatusSemNumero_consultaOsRecentesDoTelefone() {
        when(sender.isConfigured()).thenReturn(true);
        when(statusQueryService.query(any(), eq("558899999999")))
                .thenReturn(WhatsAppServiceOrderStatusQueryService.StatusQueryResult.noHistory());

        service.handleIncomingMessage("558899999999", "Maria", "Status");

        verify(sender).sendText(eq("558899999999"), contains("Não encontrei nenhum chamado seu"));
        verifyNoInteractions(intakeService);
    }

    @Test
    void comandoDeStatusComNumero_respondeComDetalheDoChamado() {
        when(sender.isConfigured()).thenReturn(true);
        ServiceOrderResponseDTO so = mock(ServiceOrderResponseDTO.class);
        when(so.id()).thenReturn(42L);
        when(so.title()).thenReturn("Notebook não liga");
        when(so.status()).thenReturn(br.com.fpo.sgc.domain.serviceorder.ServiceOrderStatus.OPEN);
        SectorResponseDTO sector = mock(SectorResponseDTO.class);
        when(sector.name()).thenReturn("TI");
        when(so.sector()).thenReturn(sector);
        when(so.technician()).thenReturn(null);
        when(so.createdAt()).thenReturn(java.time.Instant.now());
        when(statusQueryService.query(eq(new WhatsAppServiceOrderStatusQueryCommand(42L)), eq("558899999999")))
                .thenReturn(WhatsAppServiceOrderStatusQueryService.StatusQueryResult.single(so));

        service.handleIncomingMessage("558899999999", "Maria", "Status #42");

        verify(sender).sendText(eq("558899999999"), contains("#42"));
    }

    @Test
    void comandoValido_criaOsEConfirma() {
        when(sender.isConfigured()).thenReturn(true);
        SectorResponseDTO sector = mock(SectorResponseDTO.class);
        when(sector.name()).thenReturn("TI");
        ServiceOrderResponseDTO so = mock(ServiceOrderResponseDTO.class);
        when(so.id()).thenReturn(42L);
        when(so.sector()).thenReturn(sector);
        when(so.asset()).thenReturn(null);
        when(intakeService.open(any(), eq("558899999999"), eq("Maria")))
                .thenReturn(WhatsAppServiceOrderIntakeService.IntakeResult.success(so));

        service.handleIncomingMessage("558899999999", "Maria", COMANDO_VALIDO);

        verify(sender).sendText(eq("558899999999"), contains("#42"));
    }

    @Test
    void comandoComSetorInexistente_respondeComErroDeSetor() {
        when(sender.isConfigured()).thenReturn(true);
        when(intakeService.open(any(), eq("558899999999"), eq("Maria")))
                .thenReturn(WhatsAppServiceOrderIntakeService.IntakeResult.sectorNotFound("TI"));

        service.handleIncomingMessage("558899999999", "Maria", COMANDO_VALIDO);

        verify(sender).sendText(eq("558899999999"), contains("TI"));
    }

    @Test
    void comandoComBemInexistente_respondeComErroDeBem() {
        when(sender.isConfigured()).thenReturn(true);
        when(intakeService.open(any(), eq("558899999999"), eq("Maria")))
                .thenReturn(WhatsAppServiceOrderIntakeService.IntakeResult.assetNotFound("PAT-999"));

        service.handleIncomingMessage("558899999999", "Maria", COMANDO_VALIDO + "\nBem: PAT-999");

        verify(sender).sendText(eq("558899999999"), contains("PAT-999"));
    }

    @Test
    void semWhatsAppConfigurado_naoTentaEnviarNemProcessar() {
        when(sender.isConfigured()).thenReturn(false);

        service.handleIncomingMessage("558899999999", "Maria", COMANDO_VALIDO);

        verify(sender, never()).sendText(anyString(), anyString());
        verifyNoInteractions(intakeService);
    }

    @Test
    void falhaAoAbrirOsOuEnviar_naoPropagaExcecao() {
        when(sender.isConfigured()).thenReturn(true);
        when(intakeService.open(any(), anyString(), anyString())).thenThrow(new RuntimeException("falha de banco"));

        service.handleIncomingMessage("558899999999", "Maria", COMANDO_VALIDO);
    }

    @Test
    void mensagemDeAvaliacao_respondeComAConfirmacao() {
        when(sender.isConfigured()).thenReturn(true);
        when(ratingService.rate(any(), eq("558899999999")))
                .thenReturn(new WhatsAppServiceOrderRatingService.RatingResult.Success(12L, 5));

        service.handleIncomingMessage("558899999999", "Maria", "Nota #12 5");

        verify(sender).sendText(eq("558899999999"), contains("Obrigado"));
    }

    @Test
    void avaliacaoForaDoFormato_respondeComOFormato() {
        when(sender.isConfigured()).thenReturn(true);
        when(ratingService.rate(any(), any())).thenReturn(new WhatsAppServiceOrderRatingService.RatingResult.InvalidFormat());

        service.handleIncomingMessage("558899999999", "Maria", "Nota 9");

        verify(sender).sendText(eq("558899999999"), contains("Nota #<número>"));
    }

    @Test
    void avaliacaoRecusada_respondeComOMotivo() {
        when(sender.isConfigured()).thenReturn(true);
        when(ratingService.rate(any(), any()))
                .thenReturn(new WhatsAppServiceOrderRatingService.RatingResult.Rejected("Este chamado já foi avaliado."));

        service.handleIncomingMessage("558899999999", "Maria", "Nota #12 5");

        verify(sender).sendText(eq("558899999999"), contains("já foi avaliado"));
    }
}
