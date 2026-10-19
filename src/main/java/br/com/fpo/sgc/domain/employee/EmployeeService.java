package br.com.fpo.sgc.domain.employee;

import br.com.fpo.sgc.domain.employee.dto.EmployeeRequestDTO;
import br.com.fpo.sgc.domain.employee.dto.EmployeeResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EmployeeService {

    EmployeeResponseDTO create(EmployeeRequestDTO dto);

    Page<EmployeeResponseDTO> findAll(String q, Long sectorId, Boolean active, Pageable pageable);

    EmployeeResponseDTO findById(Long id);

    EmployeeResponseDTO update(Long id, EmployeeRequestDTO dto);

    void delete(Long id);
}