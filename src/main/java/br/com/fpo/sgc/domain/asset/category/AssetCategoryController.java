package br.com.fpo.sgc.domain.asset.category;

import br.com.fpo.sgc.domain.asset.category.dto.AssetCategoryRequestDTO;
import br.com.fpo.sgc.domain.asset.category.dto.AssetCategoryResponseDTO;
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
@RequestMapping("/api/v1/asset-categories")
@RequiredArgsConstructor
public class AssetCategoryController {

    private final AssetCategoryService assetCategoryService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<AssetCategoryResponseDTO> create(@RequestBody @Valid AssetCategoryRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assetCategoryService.create(dto));
    }

    @GetMapping
    public ResponseEntity<Page<AssetCategoryResponseDTO>> findAll(@PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(assetCategoryService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssetCategoryResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(assetCategoryService.findById(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<AssetCategoryResponseDTO> update(@PathVariable Long id, @RequestBody @Valid AssetCategoryRequestDTO dto) {
        return ResponseEntity.ok(assetCategoryService.update(id, dto));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        assetCategoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
