package br.com.fpo.sgc.domain.product;

import br.com.fpo.sgc.domain.product.dto.ProductRequestDTO;
import br.com.fpo.sgc.domain.product.dto.ProductResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {
    ProductResponseDTO create(ProductRequestDTO dto);
    ProductResponseDTO findById(Long id);
    Page<ProductResponseDTO> findAll(String q, Pageable pageable);
    ProductResponseDTO update(Long id, ProductRequestDTO dto);
    void delete(Long id);
}