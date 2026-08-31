package br.com.fpo.sgc.domain.dashboard;

import br.com.fpo.sgc.domain.dashboard.dto.DashboardResponseDTO;

public interface DashboardService {
    DashboardResponseDTO getDashboardData(Long companyId);
}