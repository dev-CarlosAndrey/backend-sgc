CREATE TABLE tb_buildings (
                              id BIGINT AUTO_INCREMENT PRIMARY KEY,
                              name VARCHAR(100) NOT NULL UNIQUE,
                              code VARCHAR(20),
                              active BOOLEAN NOT NULL DEFAULT TRUE,
                              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE tb_sectors (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            name VARCHAR(100) NOT NULL,
                            building_id BIGINT NOT NULL,
                            active BOOLEAN NOT NULL DEFAULT TRUE,
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
                            CONSTRAINT fk_sector_building FOREIGN KEY (building_id) REFERENCES tb_buildings(id)
);