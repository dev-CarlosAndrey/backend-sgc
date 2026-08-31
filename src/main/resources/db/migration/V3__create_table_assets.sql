CREATE TABLE tb_assets (
                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                           tag_number VARCHAR(50) NOT NULL UNIQUE, -- Código de Tombamento/Patrimônio
                           name VARCHAR(120) NOT NULL,
                           description TEXT,
                           model VARCHAR(100),
                           serial_number VARCHAR(100),
                           status VARCHAR(30) NOT NULL,
                           sector_id BIGINT NOT NULL,
                           active BOOLEAN NOT NULL DEFAULT TRUE,
                           created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
                           CONSTRAINT fk_asset_sector FOREIGN KEY (sector_id) REFERENCES tb_sectors(id)
);