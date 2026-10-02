package br.com.fpo.sgc.domain.whatsapp;

import br.com.fpo.sgc.domain.asset.Asset;
import br.com.fpo.sgc.domain.asset.AssetRepository;
import br.com.fpo.sgc.domain.location.sector.Sector;
import br.com.fpo.sgc.domain.location.sector.SectorRepository;
import br.com.fpo.sgc.domain.serviceorder.ServiceOrderChannel;
import br.com.fpo.sgc.domain.serviceorder.ServiceOrderService;
import br.com.fpo.sgc.domain.serviceorder.dto.ServiceOrderRequestDTO;
import br.com.fpo.sgc.domain.serviceorder.dto.ServiceOrderResponseDTO;
import br.com.fpo.sgc.domain.user.User;
import br.com.fpo.sgc.domain.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class WhatsAppServiceOrderIntakeServiceTest {

    private SectorRepository sectorRepository;
    private AssetRepository assetRepository;
    private UserService userService;
    private ServiceOrderService serviceOrderService;
    private WhatsAppServiceOrderIntakeService intakeService;

    private final Sector sector = Sector.builder().id(2L).name("TI").build();
    private final User requester = User.builder().id(9L).name("Contato WhatsApp 558899999999").build();

    @BeforeEach
    void setUp() {
        sectorRepository = mock(SectorRepository.class);
        assetRepository = mock(AssetRepository.class);
        userService = mock(UserService.class);
        serviceOrderService = mock(ServiceOrderService.class);
        intakeService = new WhatsAppServiceOrderIntakeService(sectorRepository, assetRepository, userService, serviceOrderService);

        when(userService.findOrCreateByPhone("558899999999", "Maria")).thenReturn(requester);
    }

    @Test
    void setorInexistente_retornaSectorNotFound() {
        when(sectorRepository.findByNameIgnoreCaseAndActiveTrue("TI")).thenReturn(Optional.empty());
        WhatsAppServiceOrderCommand command = new WhatsAppServiceOrderCommand("TI", null, "Notebook não liga");

        var result = intakeService.open(command, "558899999999", "Maria");

        assertInstanceOf(WhatsAppServiceOrderIntakeService.IntakeResult.SectorNotFound.class, result);
        verifyNoInteractions(serviceOrderService);
    }

    @Test
    void tombamentoInexistente_retornaAssetNotFound() {
        when(sectorRepository.findByNameIgnoreCaseAndActiveTrue("TI")).thenReturn(Optional.of(sector));
        when(assetRepository.findByTagNumberIgnoreCaseAndActiveTrue("PAT-999")).thenReturn(Optional.empty());
        WhatsAppServiceOrderCommand command = new WhatsAppServiceOrderCommand("TI", "PAT-999", "Notebook não liga");

        var result = intakeService.open(command, "558899999999", "Maria");

        assertInstanceOf(WhatsAppServiceOrderIntakeService.IntakeResult.AssetNotFound.class, result);
        verifyNoInteractions(serviceOrderService);
    }

    @Test
    void comandoValido_abreOsComCanalWhatsAppESemBem() {
        when(sectorRepository.findByNameIgnoreCaseAndActiveTrue("TI")).thenReturn(Optional.of(sector));
        ServiceOrderResponseDTO created = mock(ServiceOrderResponseDTO.class);
        when(serviceOrderService.create(any(), eq(ServiceOrderChannel.WHATSAPP))).thenReturn(created);
        WhatsAppServiceOrderCommand command = new WhatsAppServiceOrderCommand("TI", null, "Notebook não liga");

        var result = intakeService.open(command, "558899999999", "Maria");

        assertInstanceOf(WhatsAppServiceOrderIntakeService.IntakeResult.Success.class, result);
        assertSame(created, ((WhatsAppServiceOrderIntakeService.IntakeResult.Success) result).serviceOrder());

        ArgumentCaptor<ServiceOrderRequestDTO> dtoCaptor = ArgumentCaptor.forClass(ServiceOrderRequestDTO.class);
        verify(serviceOrderService).create(dtoCaptor.capture(), eq(ServiceOrderChannel.WHATSAPP));
        assertEquals(9L, dtoCaptor.getValue().requesterId());
        assertEquals(2L, dtoCaptor.getValue().sectorId());
        assertNull(dtoCaptor.getValue().assetId());
        assertEquals("Notebook não liga", dtoCaptor.getValue().description());
        assertTrue(dtoCaptor.getValue().title().length() >= 5);
    }

    @Test
    void comandoComBemValido_resolveEVinculaOAtivo() {
        when(sectorRepository.findByNameIgnoreCaseAndActiveTrue("TI")).thenReturn(Optional.of(sector));
        Asset asset = Asset.builder().id(7L).tagNumber("PAT-001").build();
        when(assetRepository.findByTagNumberIgnoreCaseAndActiveTrue("PAT-001")).thenReturn(Optional.of(asset));
        when(serviceOrderService.create(any(), eq(ServiceOrderChannel.WHATSAPP))).thenReturn(mock(ServiceOrderResponseDTO.class));
        WhatsAppServiceOrderCommand command = new WhatsAppServiceOrderCommand("TI", "PAT-001", "Notebook não liga");

        intakeService.open(command, "558899999999", "Maria");

        ArgumentCaptor<ServiceOrderRequestDTO> dtoCaptor = ArgumentCaptor.forClass(ServiceOrderRequestDTO.class);
        verify(serviceOrderService).create(dtoCaptor.capture(), eq(ServiceOrderChannel.WHATSAPP));
        assertEquals(7L, dtoCaptor.getValue().assetId());
    }
}
