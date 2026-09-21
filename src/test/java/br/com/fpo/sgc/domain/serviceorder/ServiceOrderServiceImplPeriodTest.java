package br.com.fpo.sgc.domain.serviceorder;

import br.com.fpo.sgc.common.exception.BusinessException;
import br.com.fpo.sgc.domain.serviceorder.mapper.ServiceOrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ServiceOrderServiceImplPeriodTest {

    private ServiceOrderRepository repository;
    private ServiceOrderServiceImpl service;
    private final Pageable pageable = PageRequest.of(0, 10);

    @BeforeEach
    void setUp() {
        repository = mock(ServiceOrderRepository.class);
        service = new ServiceOrderServiceImpl(repository, null, null, null, null, null, null, mock(ServiceOrderMapper.class));
        Page<ServiceOrder> empty = new PageImpl<>(List.of());
        when(repository.findByOpenedBetween(any(), any(), any())).thenReturn(empty);
        when(repository.findByClosedBetween(any(), any(), any())).thenReturn(empty);
    }

    @Test
    void semNenhumaData_lancaBusinessException() {
        assertThrows(BusinessException.class, () -> service.findByPeriod(null, null, ServiceOrderPeriodField.OPENED, pageable));
    }

    @Test
    void dataInicialPosteriorAFinal_lancaBusinessException() {
        assertThrows(BusinessException.class, () -> service.findByPeriod(
                LocalDate.of(2026, 9, 21), LocalDate.of(2026, 9, 20), ServiceOrderPeriodField.OPENED, pageable));
    }

    @Test
    void periodoEInclusivoNasDuasPontas_noFusoLocal() {
        service.findByPeriod(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), ServiceOrderPeriodField.OPENED, pageable);

        ArgumentCaptor<Instant> from = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> to = ArgumentCaptor.forClass(Instant.class);
        verify(repository).findByOpenedBetween(from.capture(), to.capture(), any());
        // America/Fortaleza = UTC-3 (sem horário de verão)
        assertEquals(Instant.parse("2026-09-01T03:00:00Z"), from.getValue());
        assertEquals(Instant.parse("2026-10-01T03:00:00Z"), to.getValue());
    }

    @Test
    void umaSoDataEAceita_eCampoClosedUsaAConsultaDeConclusao() {
        service.findByPeriod(LocalDate.of(2026, 9, 1), null, ServiceOrderPeriodField.CLOSED, pageable);

        ArgumentCaptor<Instant> to = ArgumentCaptor.forClass(Instant.class);
        verify(repository).findByClosedBetween(any(), to.capture(), any());
        assertNull(to.getValue());
        verify(repository, never()).findByOpenedBetween(any(), any(), any());
    }
}
