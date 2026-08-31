package br.com.fpo.sgc.domain.asset;

import br.com.fpo.sgc.domain.asset.dto.AssetRequestDTO;
import br.com.fpo.sgc.domain.asset.dto.AssetResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AssetService {
    AssetResponseDTO create(AssetRequestDTO dto);
    AssetResponseDTO findById(Long id);
    AssetResponseDTO findByTagNumber(String tagNumber);
    Page<AssetResponseDTO> findAll(Pageable pageable);
    Page<AssetResponseDTO> findBySectorId(Long sectorId, Pageable pageable);
    AssetResponseDTO update(Long id, AssetRequestDTO dto);
    void delete(Long id);
}