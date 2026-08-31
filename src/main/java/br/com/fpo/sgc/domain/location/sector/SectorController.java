package br.com.fpo.sgc.domain.location.sector;

import br.com.fpo.sgc.domain.location.sector.dto.SectorRequestDTO;
import br.com.fpo.sgc.domain.location.sector.dto.SectorResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sectors")
@RequiredArgsConstructor
public class SectorController {

    private final SectorService sectorService;

    @PostMapping
    public ResponseEntity<SectorResponseDTO> create(@RequestBody @Valid SectorRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sectorService.create(dto));
    }

    @GetMapping
    public ResponseEntity<Page<SectorResponseDTO>> findAll(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(sectorService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SectorResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(sectorService.findById(id));
    }

    @GetMapping("/building/{buildingId}")
    public ResponseEntity<Page<SectorResponseDTO>> findByBuildingId(
            @PathVariable Long buildingId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(sectorService.findByBuildingId(buildingId, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SectorResponseDTO> update(@PathVariable Long id, @RequestBody @Valid SectorRequestDTO dto) {
        return ResponseEntity.ok(sectorService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sectorService.delete(id);
        return ResponseEntity.noContent().build();
    }
}