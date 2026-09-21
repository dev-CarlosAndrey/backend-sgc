package br.com.fpo.sgc.domain.material;

import br.com.fpo.sgc.domain.material.dto.MaterialRequestDTO;
import br.com.fpo.sgc.domain.material.dto.MaterialResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/materials")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<MaterialResponseDTO> create(@RequestBody @Valid MaterialRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(materialService.create(dto));
    }

    @GetMapping
    public ResponseEntity<Page<MaterialResponseDTO>> findAll(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(materialService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaterialResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(materialService.findById(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<MaterialResponseDTO> update(@PathVariable Long id, @RequestBody @Valid MaterialRequestDTO dto) {
        return ResponseEntity.ok(materialService.update(id, dto));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        materialService.delete(id);
        return ResponseEntity.noContent().build();
    }
}