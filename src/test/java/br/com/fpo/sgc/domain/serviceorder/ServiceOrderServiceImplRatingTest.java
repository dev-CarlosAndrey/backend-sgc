package br.com.fpo.sgc.domain.serviceorder;

import br.com.fpo.sgc.common.exception.BusinessException;
import br.com.fpo.sgc.domain.audit.AuditLogService;
import br.com.fpo.sgc.domain.serviceorder.mapper.ServiceOrderMapper;
import br.com.fpo.sgc.domain.serviceorder.statuslog.ServiceOrderStatusLogRepository;
import br.com.fpo.sgc.domain.user.User;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ServiceOrderServiceImplRatingTest {

    private ServiceOrderRepository repository;
    private ServiceOrderStatusLogRepository statusLogRepository;
    private AuditLogService auditLogService;
    private ApplicationEventPublisher eventPublisher;
    private ServiceOrderServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(ServiceOrderRepository.class);
        statusLogRepository = mock(ServiceOrderStatusLogRepository.class);
        auditLogService = mock(AuditLogService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        service = new ServiceOrderServiceImpl(repository, null, null, null, statusLogRepository, auditLogService,
                eventPublisher, mock(ServiceOrderMapper.class));
    }

    private ServiceOrder givenOrder(ServiceOrderStatus status, Integer rating) {
        ServiceOrder so = ServiceOrder.builder().id(12L).status(status).rating(rating)
                .requester(User.builder().id(7L).build()).build();
        when(repository.findById(12L)).thenReturn(Optional.of(so));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        return so;
    }

    @Test
    void osConcluida_registraNotaEAuditoriaSemHistoricoNemNotificacao() {
        ServiceOrder so = givenOrder(ServiceOrderStatus.RESOLVED, null);

        service.rate(12L, 7L, 4);

        assertEquals(4, so.getRating());
        assertNotNull(so.getRatedAt());
        verify(auditLogService).log(eq("ServiceOrder"), eq(12L), eq("RATE"), any(), contains("nota 4"));
        verifyNoInteractions(statusLogRepository, eventPublisher);
    }

    @Test
    void osDeOutroSolicitante_eTratadaComoInexistente() {
        givenOrder(ServiceOrderStatus.RESOLVED, null);

        assertThrows(EntityNotFoundException.class, () -> service.rate(12L, 99L, 5));
        verify(repository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = ServiceOrderStatus.class, names = {"OPEN", "IN_PROGRESS", "CANCELLED"})
    void osNaoConcluida_naoPodeSerAvaliada(ServiceOrderStatus status) {
        givenOrder(status, null);

        BusinessException e = assertThrows(BusinessException.class, () -> service.rate(12L, 7L, 5));
        assertEquals("Só é possível avaliar um chamado concluído.", e.getMessage());
    }

    @Test
    void osJaAvaliada_naoAceitaNovaNota() {
        givenOrder(ServiceOrderStatus.RESOLVED, 3);

        BusinessException e = assertThrows(BusinessException.class, () -> service.rate(12L, 7L, 5));
        assertEquals("Este chamado já foi avaliado.", e.getMessage());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 6, -1})
    void notaForaDe1a5_eRecusada(int rating) {
        givenOrder(ServiceOrderStatus.RESOLVED, null);

        assertThrows(BusinessException.class, () -> service.rate(12L, 7L, rating));
        verify(repository, never()).save(any());
    }
}
