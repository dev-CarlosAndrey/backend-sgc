package br.com.fpo.sgc.domain.serviceorder;

import br.com.fpo.sgc.common.exception.BusinessException;
import br.com.fpo.sgc.domain.audit.AuditLogService;
import br.com.fpo.sgc.domain.serviceorder.mapper.ServiceOrderMapper;
import br.com.fpo.sgc.domain.serviceorder.statuslog.ServiceOrderStatusLogRepository;
import br.com.fpo.sgc.domain.user.User;
import br.com.fpo.sgc.domain.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ServiceOrderServiceImplResolveTest {

    private ServiceOrderRepository repository;
    private UserRepository userRepository;
    private ApplicationEventPublisher eventPublisher;
    private ServiceOrderServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(ServiceOrderRepository.class);
        userRepository = mock(UserRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        service = new ServiceOrderServiceImpl(repository, userRepository, null, null,
                mock(ServiceOrderStatusLogRepository.class), mock(AuditLogService.class), eventPublisher,
                mock(ServiceOrderMapper.class));
    }

    private void givenOrderWith(ServiceOrderStatus status) {
        when(repository.findById(1L)).thenReturn(Optional.of(ServiceOrder.builder().id(1L).status(status).build()));
    }

    @ParameterizedTest
    @EnumSource(value = ServiceOrderStatus.class, names = {"RESOLVED", "CANCELLED"})
    void osFinalizadaOuCancelada_naoPodeSerResolvida(ServiceOrderStatus status) {
        givenOrderWith(status);

        assertThrows(BusinessException.class, () -> service.resolveOrder(1L, "laudo", BigDecimal.TEN));

        verify(repository, never()).save(any());
    }

    @Test
    void mensagemDeCanceladaEEspecifica() {
        givenOrderWith(ServiceOrderStatus.CANCELLED);

        BusinessException e = assertThrows(BusinessException.class, () -> service.resolveOrder(1L, "laudo", null));

        org.junit.jupiter.api.Assertions.assertTrue(e.getMessage().contains("cancelada"));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void eventoDeResolucaoLevaOIdDeQuemResolveu() {
        givenOrderWith(ServiceOrderStatus.IN_PROGRESS);
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findByNameAndActiveTrue("Ana")).thenReturn(Optional.of(User.builder().id(42L).name("Ana").build()));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("Ana", null, java.util.List.of()));

        service.resolveOrder(1L, "laudo", null);

        ArgumentCaptor<ServiceOrderEvent> event = ArgumentCaptor.forClass(ServiceOrderEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        org.junit.jupiter.api.Assertions.assertEquals(ServiceOrderEventType.RESOLVED, event.getValue().type());
        org.junit.jupiter.api.Assertions.assertEquals(42L, event.getValue().actorUserId());
    }

    @Test
    void semUsuarioLogado_eventoVaiSemAtor() {
        givenOrderWith(ServiceOrderStatus.OPEN);
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findByNameAndActiveTrue("SYSTEM")).thenReturn(Optional.empty());

        service.resolveOrder(1L, "laudo", null);

        ArgumentCaptor<ServiceOrderEvent> event = ArgumentCaptor.forClass(ServiceOrderEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        org.junit.jupiter.api.Assertions.assertNull(event.getValue().actorUserId());
    }
}
