CREATE TABLE tb_asset_movements (
                                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                    asset_id BIGINT NOT NULL,
                                    origin_sector_id BIGINT NOT NULL,
                                    destination_sector_id BIGINT NOT NULL,
                                    user_id BIGINT,
                                    reason TEXT,
                                    movement_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                    active BOOLEAN NOT NULL DEFAULT TRUE,
                                    CONSTRAINT fk_movement_asset FOREIGN KEY (asset_id) REFERENCES tb_assets (id),
                                    CONSTRAINT fk_movement_origin_sector FOREIGN KEY (origin_sector_id) REFERENCES tb_sectors (id),
                                    CONSTRAINT fk_movement_dest_sector FOREIGN KEY (destination_sector_id) REFERENCES tb_sectors (id)
);