package br.com.fpo.sgc.domain.whatsapp;

import br.com.fpo.sgc.common.exception.BusinessException;
import br.com.fpo.sgc.domain.serviceorder.ServiceOrderService;
import br.com.fpo.sgc.domain.user.User;
import br.com.fpo.sgc.domain.user.UserRepository;
import br.com.fpo.sgc.domain.whatsapp.WhatsAppServiceOrderRatingService.RatingResult;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WhatsAppServiceOrderRatingServiceTest {

    private static final String PHONE = "558899999999";

    private final UserRepository userRepository = mock(UserRepository.class);
    private final ServiceOrderService serviceOrderService = mock(ServiceOrderService.class);
    private final WhatsAppServiceOrderRatingService service = new WhatsAppServiceOrderRatingService(userRepository, serviceOrderService);

    private void givenRequester() {
        when(userRepository.findByPhone(PHONE)).thenReturn(Optional.of(User.builder().id(7L).phone(PHONE).build()));
    }

    @Test
    void avaliacaoValida_registraComOSolicitanteDoTelefone() {
        givenRequester();

        RatingResult result = service.rate(new WhatsAppServiceOrderRatingCommand(12L, 4), PHONE);

        assertEquals(new RatingResult.Success(12L, 4), result);
        verify(serviceOrderService).rate(12L, 7L, 4);
    }

    @Test
    void comandoInvalido_naoConsultaNada() {
        RatingResult result = service.rate(new WhatsAppServiceOrderRatingCommand(null, null), PHONE);

        assertInstanceOf(RatingResult.InvalidFormat.class, result);
        verifyNoInteractions(userRepository, serviceOrderService);
    }

    @Test
    void telefoneDesconhecido_respondeQueNaoEncontrouSemCriarCadastro() {
        when(userRepository.findByPhone(PHONE)).thenReturn(Optional.empty());

        assertEquals(new RatingResult.NotFound(12L), service.rate(new WhatsAppServiceOrderRatingCommand(12L, 5), PHONE));
        verify(serviceOrderService, never()).rate(anyLong(), anyLong(), anyInt());
    }

    @Test
    void osInexistenteOuDeOutraPessoa_respondeQueNaoEncontrou() {
        givenRequester();
        when(serviceOrderService.rate(12L, 7L, 5)).thenThrow(new EntityNotFoundException("não encontrada"));

        assertEquals(new RatingResult.NotFound(12L), service.rate(new WhatsAppServiceOrderRatingCommand(12L, 5), PHONE));
    }

    @Test
    void regraDeNegocio_devolveOMotivo() {
        givenRequester();
        when(serviceOrderService.rate(12L, 7L, 5)).thenThrow(new BusinessException("Este chamado já foi avaliado."));

        assertEquals(new RatingResult.Rejected("Este chamado já foi avaliado."),
                service.rate(new WhatsAppServiceOrderRatingCommand(12L, 5), PHONE));
    }
}
