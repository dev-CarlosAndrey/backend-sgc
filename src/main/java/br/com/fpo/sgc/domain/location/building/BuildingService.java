package br.com.fpo.sgc.domain.location.building;

import br.com.fpo.sgc.domain.location.building.dto.BuildingRequestDTO;
import br.com.fpo.sgc.domain.location.building.dto.BuildingResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BuildingService {
    BuildingResponseDTO create(BuildingRequestDTO dto);
    BuildingResponseDTO findById(Long id);
    Page<BuildingResponseDTO> findAll(Pageable pageable);
    Page<BuildingResponseDTO> findByCompany(Long companyId, Pageable pageable); // <-- Adicionado para a tela
    BuildingResponseDTO update(Long id, BuildingRequestDTO dto);
    void delete(Long id);
}