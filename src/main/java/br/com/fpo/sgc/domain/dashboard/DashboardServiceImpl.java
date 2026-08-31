package br.com.fpo.sgc.domain.dashboard;

import br.com.fpo.sgc.domain.asset.AssetRepository;
import br.com.fpo.sgc.domain.asset.AssetStatus;
import br.com.fpo.sgc.domain.dashboard.dto.DashboardResponseDTO;
import br.com.fpo.sgc.domain.dashboard.mapper.DashboardMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final AssetRepository assetRepository;
    private final DashboardMapper dashboardMapper;

    @Override
    @Transactional(readOnly = true)
    public DashboardResponseDTO getDashboardData(Long companyId) {
        long totalAssets = assetRepository.countByCompanyId(companyId);

        Map<String, Long> assetsByCompany = new HashMap<>();
        List<Object[]> companyResults = assetRepository.countAssetsByCompany(companyId);
        for (Object[] result : companyResults) {
            String companyName = (String) result[0];
            Long count = (Long) result[1];
            if (companyName != null) {
                assetsByCompany.put(companyName, count);
            }
        }

        Map<String, Long> statusCount = new HashMap<>();
        for (AssetStatus status : AssetStatus.values()) {
            long count = assetRepository.findByStatusAndActiveTrue(status, null).getTotalElements();
            statusCount.put(status.name(), count);
        }

        long activeTickets = 12;
        double maintenanceCost = 8450.00;
        long depreciatingAssets = 36;

        return dashboardMapper.toDTO(
                totalAssets,
                activeTickets,
                maintenanceCost,
                depreciatingAssets,
                statusCount,
                assetsByCompany
        );
    }
}