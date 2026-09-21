package br.com.fpo.sgc.domain.company;

import br.com.fpo.sgc.domain.company.dto.CompanyRequestDTO;
import br.com.fpo.sgc.domain.company.dto.CompanyResponseDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
@Tag(name = "Empresas", description = "Endpoints para gestão de empresas/instituições")
public class CompanyController {

    private final CompanyService companyService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<CompanyResponseDTO> create(@Valid @RequestBody CompanyRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(companyService.create(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompanyResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(companyService.findById(id));
    }

    @GetMapping
    public ResponseEntity<Page<CompanyResponseDTO>> findAll(Pageable pageable) {
        return ResponseEntity.ok(companyService.findAll(pageable));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<CompanyResponseDTO> update(@PathVariable Long id, @Valid @RequestBody CompanyRequestDTO dto) {
        return ResponseEntity.ok(companyService.update(id, dto));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        companyService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CompanyResponseDTO> uploadLogo(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(companyService.uploadLogo(id, file));
    }
}