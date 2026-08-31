package br.com.fpo.sgc.domain.material;

import br.com.fpo.sgc.domain.material.dto.MaterialRequestDTO;
import br.com.fpo.sgc.domain.material.dto.MaterialResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MaterialService {
    MaterialResponseDTO create(MaterialRequestDTO dto);
    MaterialResponseDTO findById(Long id);
    Page<MaterialResponseDTO> findAll(Pageable pageable);
    MaterialResponseDTO update(Long id, MaterialRequestDTO dto);
    void delete(Long id);
}