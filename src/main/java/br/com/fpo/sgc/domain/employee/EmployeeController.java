package br.com.fpo.sgc.domain.employee;

import br.com.fpo.sgc.domain.employee.dto.EmployeeRequestDTO;
import br.com.fpo.sgc.domain.employee.dto.EmployeeResponseDTO;
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
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<EmployeeResponseDTO> create(@RequestBody @Valid EmployeeRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.create(dto));
    }

    @GetMapping
    public ResponseEntity<Page<EmployeeResponseDTO>> findAll(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long sectorId,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(employeeService.findAll(q, sectorId, active, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.findById(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<EmployeeResponseDTO> update(@PathVariable Long id, @RequestBody @Valid EmployeeRequestDTO dto) {
        return ResponseEntity.ok(employeeService.update(id, dto));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        employeeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}