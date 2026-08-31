package br.com.fpo.sgc.domain.product;

import br.com.fpo.sgc.domain.audit.AuditLogService;
import br.com.fpo.sgc.domain.brand.Brand;
import br.com.fpo.sgc.domain.brand.BrandRepository;
import br.com.fpo.sgc.domain.material.Material;
import br.com.fpo.sgc.domain.material.MaterialRepository;
import br.com.fpo.sgc.domain.product.dto.ProductRequestDTO;
import br.com.fpo.sgc.domain.product.dto.ProductResponseDTO;
import br.com.fpo.sgc.domain.product.mapper.ProductMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final MaterialRepository materialRepository;
    private final ProductMapper productMapper;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public ProductResponseDTO create(ProductRequestDTO dto) {
        Brand brand = brandRepository.findById(dto.brandId())
                .orElseThrow(() -> new EntityNotFoundException("Marca não encontrada com ID: " + dto.brandId()));

        Material material = materialRepository.findById(dto.materialId())
                .orElseThrow(() -> new EntityNotFoundException("Material não encontrado com ID: " + dto.materialId()));

        Product product = productMapper.toEntity(dto, brand, material);
        Product savedProduct = productRepository.save(product);

        auditLogService.log(
                "Product",
                savedProduct.getId(),
                "CREATE",
                savedProduct.getName(),
                "Produto " + savedProduct.getName() + " criado com Marca " + brand.getName()
        );

        return productMapper.toDTO(savedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponseDTO findById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Produto não encontrado com ID: " + id));

        return productMapper.toDTO(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> findAll(Pageable pageable) {
        return productRepository.findByActiveTrue(pageable)
                .map(productMapper::toDTO);
    }

    @Override
    @Transactional
    public ProductResponseDTO update(Long id, ProductRequestDTO dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Produto não encontrado com ID: " + id));

        Brand brand = brandRepository.findById(dto.brandId())
                .orElseThrow(() -> new EntityNotFoundException("Marca não encontrada com ID: " + dto.brandId()));

        Material material = materialRepository.findById(dto.materialId())
                .orElseThrow(() -> new EntityNotFoundException("Material não encontrado com ID: " + dto.materialId()));

        productMapper.updateEntityFromDto(product, dto, brand, material);
        Product updatedProduct = productRepository.save(product);

        auditLogService.log(
                "Product",
                updatedProduct.getId(),
                "UPDATE",
                updatedProduct.getName(),
                "Produto " + updatedProduct.getName() + " atualizado"
        );

        return productMapper.toDTO(updatedProduct);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Produto não encontrado com ID: " + id));

        product.setActive(false);
        productRepository.save(product);

        auditLogService.log(
                "Product",
                id,
                "DELETE",
                product.getName(),
                "Produto " + product.getName() + " inativado"
        );
    }
}