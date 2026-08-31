CREATE TABLE tb_companies (
                              id BIGINT AUTO_INCREMENT PRIMARY KEY,
                              company_name VARCHAR(150) NOT NULL,
                              cnpj VARCHAR(18) NOT NULL UNIQUE,
                              email VARCHAR(100) NOT NULL,
                              phone VARCHAR(20) NOT NULL,
                              logo_url VARCHAR(255),
                              active BOOLEAN NOT NULL DEFAULT TRUE
);