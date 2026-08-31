package br.com.fpo.sgc.domain.product.mapper;

import br.com.fpo.sgc.domain.brand.Brand;
import br.com.fpo.sgc.domain.material.Material;
import br.com.fpo.sgc.domain.product.Product;
import br.com.fpo.sgc.domain.product.dto.ProductRequestDTO;
import br.com.fpo.sgc.domain.product.dto.ProductResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public Product toEntity(ProductRequestDTO dto, Brand brand, Material material) {
        if (dto == null) return null;

        return Product.builder()
                .name(dto.name())
                .description(dto.description())
                .brand(brand)
                .material(material)
                .build();
    }

    public ProductResponseDTO toDTO(Product product) {
        if (product == null) return null;

        return new ProductResponseDTO(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getBrand() != null ? product.getBrand().getId() : null,
                product.getBrand() != null ? product.getBrand().getName() : null,
                product.getMaterial() != null ? product.getMaterial().getId() : null,
                product.getMaterial() != null ? product.getMaterial().getName() : null,
                product.getMaterial() != null ? product.getMaterial().getCategory() : null,
                product.getActive(),
                product.getCreatedAt()
        );
    }

    public void updateEntityFromDto(Product product, ProductRequestDTO dto, Brand brand, Material material) {
        if (product == null || dto == null) return;

        product.setName(dto.name());
        product.setDescription(dto.description());
        product.setBrand(brand);
        product.setMaterial(material);
        if (dto.active() != null) {
            product.setActive(dto.active());
        }
    }
}