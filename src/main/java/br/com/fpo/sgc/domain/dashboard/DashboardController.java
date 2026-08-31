package br.com.fpo.sgc.domain.dashboard;

import br.com.fpo.sgc.domain.dashboard.dto.DashboardResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<DashboardResponseDTO> getDashboardData(
            @RequestParam(required = false) Long companyId
    ) {
        return ResponseEntity.ok(dashboardService.getDashboardData(companyId));
    }
}