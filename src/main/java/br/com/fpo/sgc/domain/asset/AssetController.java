package br.com.fpo.sgc.domain.asset;

import br.com.fpo.sgc.domain.asset.dto.AssetRequestDTO;
import br.com.fpo.sgc.domain.asset.dto.AssetResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    @PostMapping
    public ResponseEntity<AssetResponseDTO> create(@Valid @RequestBody AssetRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assetService.create(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssetResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(assetService.findById(id));
    }

    @GetMapping("/tag/{tagNumber}")
    public ResponseEntity<AssetResponseDTO> findByTagNumber(@PathVariable String tagNumber) {
        return ResponseEntity.ok(assetService.findByTagNumber(tagNumber));
    }

    @GetMapping
    public ResponseEntity<Page<AssetResponseDTO>> findAll(
            @PageableDefault(size = 10, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(assetService.findAll(pageable));
    }

    @GetMapping("/sector/{sectorId}")
    public ResponseEntity<Page<AssetResponseDTO>> findBySectorId(
            @PathVariable Long sectorId,
            @PageableDefault(size = 10, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(assetService.findBySectorId(sectorId, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AssetResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody AssetRequestDTO dto) {
        return ResponseEntity.ok(assetService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        assetService.delete(id);
        return ResponseEntity.noContent().build();
    }
}