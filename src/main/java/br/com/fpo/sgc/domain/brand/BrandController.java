package br.com.fpo.sgc.domain.brand;

import br.com.fpo.sgc.domain.brand.dto.BrandRequestDTO;
import br.com.fpo.sgc.domain.brand.dto.BrandResponseDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/brands")
@RequiredArgsConstructor
@Tag(name = "Marcas", description = "Endpoints para gestão de marcas de equipamentos")
public class BrandController {

    private final BrandService brandService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<BrandResponseDTO> create(@Valid @RequestBody BrandRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(brandService.create(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BrandResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(brandService.findById(id));
    }

    @GetMapping
    public ResponseEntity<Page<BrandResponseDTO>> findAll(
            @RequestParam(required = false) String q,
            Pageable pageable) {
        return ResponseEntity.ok(brandService.findAll(q, pageable));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<BrandResponseDTO> update(@PathVariable Long id, @Valid @RequestBody BrandRequestDTO dto) {
        return ResponseEntity.ok(brandService.update(id, dto));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        brandService.delete(id);
        return ResponseEntity.noContent().build();
    }
}