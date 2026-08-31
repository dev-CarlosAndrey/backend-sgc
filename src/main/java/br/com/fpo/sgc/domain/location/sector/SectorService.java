package br.com.fpo.sgc.domain.location.sector;

import br.com.fpo.sgc.domain.location.sector.dto.SectorRequestDTO;
import br.com.fpo.sgc.domain.location.sector.dto.SectorResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SectorService {
    SectorResponseDTO create(SectorRequestDTO dto);
    SectorResponseDTO findById(Long id);
    Page<SectorResponseDTO> findAll(Pageable pageable);
    Page<SectorResponseDTO> findByBuildingId(Long buildingId, Pageable pageable);
    SectorResponseDTO update(Long id, SectorRequestDTO dto);
    void delete(Long id);
}