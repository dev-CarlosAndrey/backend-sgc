package br.com.fpo.sgc.domain.location.building;

import br.com.fpo.sgc.domain.location.building.dto.BuildingRequestDTO;
import br.com.fpo.sgc.domain.location.building.dto.BuildingResponseDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/buildings")
@RequiredArgsConstructor
@Tag(name = "Prédios", description = "Endpoints para gestão de prédios e infraestrutura física")
public class BuildingController {

    private final BuildingService buildingService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<BuildingResponseDTO> create(@Valid @RequestBody BuildingRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(buildingService.create(dto));
    }

    // --- NOVO: Listar todos os prédios (Paginado) ---
    @GetMapping
    public ResponseEntity<Page<BuildingResponseDTO>> findAll(Pageable pageable) {
        return ResponseEntity.ok(buildingService.findAll(pageable));
    }

    // --- NOVO: Buscar prédio por ID ---
    @GetMapping("/{id}")
    public ResponseEntity<BuildingResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(buildingService.findById(id));
    }

    @GetMapping("/company/{companyId}")
    public ResponseEntity<Page<BuildingResponseDTO>> findByCompany(@PathVariable Long companyId, Pageable pageable) {
        return ResponseEntity.ok(buildingService.findByCompany(companyId, pageable));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<BuildingResponseDTO> update(@PathVariable Long id, @Valid @RequestBody BuildingRequestDTO dto) {
        return ResponseEntity.ok(buildingService.update(id, dto));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        buildingService.delete(id);
        return ResponseEntity.noContent().build();
    }
}