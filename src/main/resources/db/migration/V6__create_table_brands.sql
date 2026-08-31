CREATE TABLE tb_brands (
                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                           name VARCHAR(100) NOT NULL UNIQUE,
                           active BOOLEAN NOT NULL DEFAULT TRUE
);