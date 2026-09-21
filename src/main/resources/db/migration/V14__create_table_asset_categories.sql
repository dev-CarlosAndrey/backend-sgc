CREATE TABLE tb_asset_categories (
                                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                      name VARCHAR(100) NOT NULL UNIQUE,
                                      description VARCHAR(255),
                                      active BOOLEAN NOT NULL DEFAULT TRUE,
                                      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP
);

ALTER TABLE tb_assets
    ADD COLUMN category_id BIGINT,
ADD CONSTRAINT fk_asset_category FOREIGN KEY (category_id) REFERENCES tb_asset_categories (id);
