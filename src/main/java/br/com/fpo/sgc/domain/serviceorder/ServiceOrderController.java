package br.com.fpo.sgc.domain.serviceorder;

import br.com.fpo.sgc.domain.serviceorder.dto.ServiceOrderRequestDTO;
import br.com.fpo.sgc.domain.serviceorder.dto.ServiceOrderResponseDTO;
import br.com.fpo.sgc.domain.serviceorder.dto.ServiceOrderStatusChangeRequestDTO;
import br.com.fpo.sgc.domain.serviceorder.statuslog.dto.ServiceOrderStatusLogResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/service-orders")
@RequiredArgsConstructor
public class ServiceOrderController {

    private final ServiceOrderService serviceOrderService;

    @PostMapping
    public ResponseEntity<ServiceOrderResponseDTO> create(@Valid @RequestBody ServiceOrderRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceOrderService.create(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceOrderResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(serviceOrderService.findById(id));
    }

    @GetMapping
    public ResponseEntity<Page<ServiceOrderResponseDTO>> findAll(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) ServiceOrderStatus status,
            @RequestParam(required = false) ServiceOrderChannel channel,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(serviceOrderService.findAll(q, status, channel, pageable));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'TECHNICIAN')")
    @GetMapping("/asset/{assetId}")
    public ResponseEntity<Page<ServiceOrderResponseDTO>> findByAssetId(
            @PathVariable Long assetId,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(serviceOrderService.findByAssetId(assetId, pageable));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'TECHNICIAN')")
    @GetMapping("/status/{status}")
    public ResponseEntity<Page<ServiceOrderResponseDTO>> findByStatus(
            @PathVariable ServiceOrderStatus status,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(serviceOrderService.findByStatus(status, pageable));
    }

    @GetMapping("/requester/{requesterId}")
    public ResponseEntity<Page<ServiceOrderResponseDTO>> findByRequesterId(
            @PathVariable Long requesterId,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(serviceOrderService.findByRequesterId(requesterId, pageable));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'TECHNICIAN')")
    @GetMapping("/technician/{technicianId}")
    public ResponseEntity<Page<ServiceOrderResponseDTO>> findByTechnicianId(
            @PathVariable Long technicianId,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(serviceOrderService.findByTechnicianId(technicianId, pageable));
    }

    // Datas inclusivas (yyyy-MM-dd); field = OPENED (data de abertura, padrão) ou CLOSED (data de conclusão/cancelamento).
    @PreAuthorize("hasAnyRole('ADMIN', 'TECHNICIAN')")
    @GetMapping("/period")
    public ResponseEntity<Page<ServiceOrderResponseDTO>> findByPeriod(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "OPENED") ServiceOrderPeriodField field,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(serviceOrderService.findByPeriod(from, to, field, pageable));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/assign-technician/{technicianId}")
    public ResponseEntity<ServiceOrderResponseDTO> assignTechnician(
            @PathVariable Long id,
            @PathVariable Long technicianId) {
        return ResponseEntity.ok(serviceOrderService.assignTechnician(id, technicianId));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'TECHNICIAN')")
    @PatchMapping("/{id}/resolve")
    public ResponseEntity<ServiceOrderResponseDTO> resolveOrder(
            @PathVariable Long id,
            @RequestBody String solutionDetails,
            @RequestParam(required = false) BigDecimal cost) {
        return ResponseEntity.ok(serviceOrderService.resolveOrder(id, solutionDetails, cost));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'TECHNICIAN')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ServiceOrderResponseDTO> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody ServiceOrderStatusChangeRequestDTO dto) {
        return ResponseEntity.ok(serviceOrderService.changeStatus(id, dto));
    }

    @GetMapping("/{id}/status-history")
    public ResponseEntity<Page<ServiceOrderStatusLogResponseDTO>> findStatusHistory(
            @PathVariable Long id,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(serviceOrderService.findStatusHistory(id, pageable));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        serviceOrderService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
