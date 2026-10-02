package br.com.fpo.sgc.domain.serviceorder;

import br.com.fpo.sgc.domain.audit.AuditLogService;
import br.com.fpo.sgc.domain.location.sector.Sector;
import br.com.fpo.sgc.domain.location.sector.SectorRepository;
import br.com.fpo.sgc.domain.serviceorder.dto.ServiceOrderRequestDTO;
import br.com.fpo.sgc.domain.serviceorder.mapper.ServiceOrderMapper;
import br.com.fpo.sgc.domain.serviceorder.statuslog.ServiceOrderStatusLogRepository;
import br.com.fpo.sgc.domain.user.User;
import br.com.fpo.sgc.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// create(dto) é o usado pelo painel (sempre PANEL); create(dto, channel) é o overload usado pela abertura
// via WhatsApp (RF15) para marcar WHATSAPP. Ambos compartilham a mesma lógica interna.
class ServiceOrderServiceImplChannelTest {

    private ServiceOrderRepository repository;
    private UserRepository userRepository;
    private SectorRepository sectorRepository;
    private ServiceOrderMapper mapper;
    private ServiceOrderServiceImpl service;

    private final User requester = User.builder().id(1L).name("Ana").build();
    private final Sector sector = Sector.builder().id(2L).name("TI").build();
    private final ServiceOrderRequestDTO dto = new ServiceOrderRequestDTO(
            "Notebook não liga", "Descrição", ServiceOrderPriority.MEDIUM, 1L, null, 2L);

    @BeforeEach
    void setUp() {
        repository = mock(ServiceOrderRepository.class);
        userRepository = mock(UserRepository.class);
        sectorRepository = mock(SectorRepository.class);
        mapper = mock(ServiceOrderMapper.class);
        service = new ServiceOrderServiceImpl(repository, userRepository, sectorRepository, null,
                mock(ServiceOrderStatusLogRepository.class), mock(AuditLogService.class),
                mock(ApplicationEventPublisher.class), mapper);

        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(sectorRepository.findById(2L)).thenReturn(Optional.of(sector));
        when(mapper.toEntity(dto, requester, sector, null))
                .thenReturn(ServiceOrder.builder().title(dto.title()).description(dto.description())
                        .priority(dto.priority()).requester(requester).sector(sector).build());
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void create_semCanalExplicito_marcaPanel() {
        service.create(dto);

        assertEquals(ServiceOrderChannel.PANEL, capturedChannel());
    }

    @Test
    void create_comCanalWhatsApp_marcaWhatsApp() {
        service.create(dto, ServiceOrderChannel.WHATSAPP);

        assertEquals(ServiceOrderChannel.WHATSAPP, capturedChannel());
    }

    private ServiceOrderChannel capturedChannel() {
        ArgumentCaptor<ServiceOrder> captor = ArgumentCaptor.forClass(ServiceOrder.class);
        verify(repository).save(captor.capture());
        return captor.getValue().getChannel();
    }
}
