package br.com.fpo.sgc.domain.whatsapp;

import br.com.fpo.sgc.domain.serviceorder.ServiceOrderService;
import br.com.fpo.sgc.domain.serviceorder.dto.ServiceOrderResponseDTO;
import br.com.fpo.sgc.domain.user.User;
import br.com.fpo.sgc.domain.user.UserRepository;
import br.com.fpo.sgc.domain.user.dto.UserResponseDTO;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WhatsAppServiceOrderStatusQueryServiceTest {

    private UserRepository userRepository;
    private ServiceOrderService serviceOrderService;
    private WhatsAppServiceOrderStatusQueryService queryService;

    private final User requester = User.builder().id(9L).phone("558899999999").build();

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        serviceOrderService = mock(ServiceOrderService.class);
        queryService = new WhatsAppServiceOrderStatusQueryService(userRepository, serviceOrderService);
    }

    @Test
    void telefoneDesconhecido_naoCriaUsuarioERetornaSemHistorico() {
        when(userRepository.findByPhone("558899999999")).thenReturn(Optional.empty());

        var result = queryService.query(new WhatsAppServiceOrderStatusQueryCommand(null), "558899999999");

        assertInstanceOf(WhatsAppServiceOrderStatusQueryService.StatusQueryResult.NoHistory.class, result);
        verifyNoInteractions(serviceOrderService);
    }

    @Test
    void semNumero_semChamadosAnteriores_retornaSemHistorico() {
        when(userRepository.findByPhone("558899999999")).thenReturn(Optional.of(requester));
        when(serviceOrderService.findByRequesterId(eq(9L), any())).thenReturn(Page.empty());

        var result = queryService.query(new WhatsAppServiceOrderStatusQueryCommand(null), "558899999999");

        assertInstanceOf(WhatsAppServiceOrderStatusQueryService.StatusQueryResult.NoHistory.class, result);
    }

    @Test
    void semNumero_comChamados_retornaListaRecente() {
        when(userRepository.findByPhone("558899999999")).thenReturn(Optional.of(requester));
        ServiceOrderResponseDTO so = mock(ServiceOrderResponseDTO.class);
        when(serviceOrderService.findByRequesterId(eq(9L), any())).thenReturn(new PageImpl<>(List.of(so)));

        var result = queryService.query(new WhatsAppServiceOrderStatusQueryCommand(null), "558899999999");

        assertInstanceOf(WhatsAppServiceOrderStatusQueryService.StatusQueryResult.RecentList.class, result);
        assertEquals(List.of(so), ((WhatsAppServiceOrderStatusQueryService.StatusQueryResult.RecentList) result).serviceOrders());
    }

    @Test
    void comNumero_osDoProprioTelefone_retornaDetalhe() {
        when(userRepository.findByPhone("558899999999")).thenReturn(Optional.of(requester));
        ServiceOrderResponseDTO so = mock(ServiceOrderResponseDTO.class);
        UserResponseDTO requesterDto = mock(UserResponseDTO.class);
        when(requesterDto.id()).thenReturn(9L);
        when(so.requester()).thenReturn(requesterDto);
        when(serviceOrderService.findById(42L)).thenReturn(so);

        var result = queryService.query(new WhatsAppServiceOrderStatusQueryCommand(42L), "558899999999");

        assertInstanceOf(WhatsAppServiceOrderStatusQueryService.StatusQueryResult.Single.class, result);
        assertSame(so, ((WhatsAppServiceOrderStatusQueryService.StatusQueryResult.Single) result).serviceOrder());
    }

    @Test
    void comNumero_osDeOutroUsuario_retornaNaoEncontrado() {
        when(userRepository.findByPhone("558899999999")).thenReturn(Optional.of(requester));
        ServiceOrderResponseDTO so = mock(ServiceOrderResponseDTO.class);
        UserResponseDTO outroRequesterDto = mock(UserResponseDTO.class);
        when(outroRequesterDto.id()).thenReturn(999L);
        when(so.requester()).thenReturn(outroRequesterDto);
        when(serviceOrderService.findById(42L)).thenReturn(so);

        var result = queryService.query(new WhatsAppServiceOrderStatusQueryCommand(42L), "558899999999");

        assertInstanceOf(WhatsAppServiceOrderStatusQueryService.StatusQueryResult.NotFound.class, result);
        assertEquals(42L, ((WhatsAppServiceOrderStatusQueryService.StatusQueryResult.NotFound) result).serviceOrderId());
    }

    @Test
    void comNumero_osInexistente_retornaNaoEncontrado() {
        when(userRepository.findByPhone("558899999999")).thenReturn(Optional.of(requester));
        when(serviceOrderService.findById(999L)).thenThrow(new EntityNotFoundException("não encontrada"));

        var result = queryService.query(new WhatsAppServiceOrderStatusQueryCommand(999L), "558899999999");

        assertInstanceOf(WhatsAppServiceOrderStatusQueryService.StatusQueryResult.NotFound.class, result);
    }
}
