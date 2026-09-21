package br.com.fpo.sgc.domain.asset.category;

import br.com.fpo.sgc.domain.asset.category.dto.AssetCategoryRequestDTO;
import br.com.fpo.sgc.domain.asset.category.dto.AssetCategoryResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AssetCategoryService {
    AssetCategoryResponseDTO create(AssetCategoryRequestDTO dto);
    AssetCategoryResponseDTO findById(Long id);
    Page<AssetCategoryResponseDTO> findAll(Pageable pageable);
    AssetCategoryResponseDTO update(Long id, AssetCategoryRequestDTO dto);
    void delete(Long id);
}
