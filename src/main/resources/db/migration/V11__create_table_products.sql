CREATE TABLE tb_products (
                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                             name VARCHAR(150) NOT NULL,
                             description TEXT,
                             brand_id BIGINT NOT NULL,
                             material_id BIGINT NOT NULL,
                             active BOOLEAN NOT NULL DEFAULT TRUE,
                             created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                             updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                             CONSTRAINT fk_product_brand FOREIGN KEY (brand_id) REFERENCES tb_brands (id),
                             CONSTRAINT fk_product_material FOREIGN KEY (material_id) REFERENCES tb_materials (id)
);