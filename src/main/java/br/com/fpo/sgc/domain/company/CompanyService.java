package br.com.fpo.sgc.domain.company;

import br.com.fpo.sgc.domain.company.dto.CompanyRequestDTO;
import br.com.fpo.sgc.domain.company.dto.CompanyResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface CompanyService {
    CompanyResponseDTO create(CompanyRequestDTO dto);
    CompanyResponseDTO findById(Long id);
    Page<CompanyResponseDTO> findAll(Pageable pageable);
    CompanyResponseDTO update(Long id, CompanyRequestDTO dto);
    void delete(Long id);
    CompanyResponseDTO uploadLogo(Long id, MultipartFile file);
}