package br.com.fpo.sgc.domain.asset.movement;

import br.com.fpo.sgc.domain.asset.movement.dto.AssetMovementRequestDTO;
import br.com.fpo.sgc.domain.asset.movement.dto.AssetMovementResponseDTO;
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
@RequestMapping("/api/v1/asset-movements")
@RequiredArgsConstructor
public class AssetMovementController {

    private final AssetMovementService movementService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<AssetMovementResponseDTO> create(@Valid @RequestBody AssetMovementRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(movementService.create(dto));
    }

    @GetMapping
    public ResponseEntity<Page<AssetMovementResponseDTO>> findAll(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(movementService.findAll(pageable));
    }

    @GetMapping("/asset/{assetId}")
    public ResponseEntity<Page<AssetMovementResponseDTO>> findByAssetId(
            @PathVariable Long assetId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(movementService.findByAssetId(assetId, pageable));
    }
}