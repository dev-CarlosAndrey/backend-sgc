package br.com.fpo.sgc.domain.serviceorder;

import br.com.fpo.sgc.common.exception.BusinessException;
import br.com.fpo.sgc.domain.serviceorder.mapper.ServiceOrderMapper;
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
    private ServiceOrderServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(ServiceOrderRepository.class);
        service = new ServiceOrderServiceImpl(repository, null, null, null, null, null, null, mock(ServiceOrderMapper.class));
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
}
