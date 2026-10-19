package br.com.fpo.sgc.domain.asset;

import br.com.fpo.sgc.domain.asset.dto.AssetRequestDTO;
import br.com.fpo.sgc.domain.asset.dto.AssetResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface AssetService {
    AssetResponseDTO create(AssetRequestDTO dto);
    AssetResponseDTO findById(Long id);
    AssetResponseDTO findByTagNumber(String tagNumber);
    Page<AssetResponseDTO> findAll(String q, AssetStatus status, Long sectorId, Long categoryId, Pageable pageable);
    Page<AssetResponseDTO> findBySectorId(Long sectorId, Pageable pageable);
    Page<AssetResponseDTO> findByStatus(AssetStatus status, Pageable pageable);
    Page<AssetResponseDTO> findByCategoryId(Long categoryId, Pageable pageable);
    Page<AssetResponseDTO> findByResponsibleId(Long responsibleId, Pageable pageable);
    AssetResponseDTO update(Long id, AssetRequestDTO dto);
    void delete(Long id);
    AssetResponseDTO uploadPhoto(Long id, MultipartFile file);
}
