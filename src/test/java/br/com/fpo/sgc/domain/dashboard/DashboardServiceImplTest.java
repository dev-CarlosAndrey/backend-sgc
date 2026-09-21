package br.com.fpo.sgc.domain.dashboard;

import br.com.fpo.sgc.domain.asset.AssetRepository;
import br.com.fpo.sgc.domain.asset.AssetStatus;
import br.com.fpo.sgc.domain.dashboard.dto.DashboardResponseDTO;
import br.com.fpo.sgc.domain.dashboard.mapper.DashboardMapper;
import br.com.fpo.sgc.domain.serviceorder.ServiceOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class DashboardServiceImplTest {

    private AssetRepository assetRepository;
    private ServiceOrderRepository serviceOrderRepository;
    private DashboardServiceImpl service;

    @BeforeEach
    void setUp() {
        assetRepository = mock(AssetRepository.class);
        serviceOrderRepository = mock(ServiceOrderRepository.class);
        when(serviceOrderRepository.sumMaintenanceCostByCompanyId(any())).thenReturn(BigDecimal.ZERO);
        when(assetRepository.countAssetsByCompany(any())).thenReturn(List.of());
        service = new DashboardServiceImpl(assetRepository, serviceOrderRepository, new DashboardMapper());
    }

    @Test
    void assetsByStatus_respeitaOFiltroDeEmpresa() {
        when(assetRepository.countByStatusAndCompanyId(7L)).thenReturn(List.<Object[]>of(
                new Object[]{AssetStatus.IN_USE, 3L}, new Object[]{AssetStatus.AVAILABLE, 1L}));

        DashboardResponseDTO dto = service.getDashboardData(7L);

        verify(assetRepository).countByStatusAndCompanyId(7L);
        assertEquals(3L, dto.assetsByStatus().get("IN_USE"));
        assertEquals(1L, dto.assetsByStatus().get("AVAILABLE"));
    }

    @Test
    void assetsByStatus_trazTodasAsSituacoesComZeros() {
        when(assetRepository.countByStatusAndCompanyId(null)).thenReturn(List.<Object[]>of(
                new Object[]{AssetStatus.DISCARDED, 2L}));

        DashboardResponseDTO dto = service.getDashboardData(null);

        assertEquals(4, dto.assetsByStatus().size());
        assertEquals(0L, dto.assetsByStatus().get("IN_USE"));
        assertEquals(2L, dto.assetsByStatus().get("DISCARDED"));
    }
}
