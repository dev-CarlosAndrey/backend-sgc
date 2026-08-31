-- 1. Criação da tabela de funcionários
CREATE TABLE tb_employees (
                              id BIGINT AUTO_INCREMENT PRIMARY KEY,
                              name VARCHAR(150) NOT NULL,
                              cpf VARCHAR(14) NOT NULL UNIQUE,
                              registration_number VARCHAR(50) NOT NULL UNIQUE,
                              email VARCHAR(100),
                              phone VARCHAR(20),
                              position VARCHAR(100),
                              admission_date DATE,
                              active BOOLEAN NOT NULL DEFAULT TRUE,
                              company_id BIGINT NOT NULL,
                              sector_id BIGINT,
                              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                              CONSTRAINT fk_employee_company FOREIGN KEY (company_id) REFERENCES tb_companies (id),
                              CONSTRAINT fk_employee_sector FOREIGN KEY (sector_id) REFERENCES tb_sectors (id)
);

-- 2. Alteração na tabela de setores para adicionar a coluna do responsável (manager)
ALTER TABLE tb_sectors
    ADD COLUMN manager_employee_id BIGINT,
ADD CONSTRAINT fk_sector_manager FOREIGN KEY (manager_employee_id) REFERENCES tb_employees (id);