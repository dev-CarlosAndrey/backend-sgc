package br.com.fpo.sgc.domain.brand;

import br.com.fpo.sgc.domain.brand.dto.BrandRequestDTO;
import br.com.fpo.sgc.domain.brand.dto.BrandResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BrandService {
    BrandResponseDTO create(BrandRequestDTO dto);
    BrandResponseDTO findById(Long id);
    Page<BrandResponseDTO> findAll(String q, Pageable pageable);
    BrandResponseDTO update(Long id, BrandRequestDTO dto);
    void delete(Long id);
}