-- 1. Adiciona a coluna de endereço
ALTER TABLE tb_buildings
    ADD COLUMN address VARCHAR(255) NOT NULL AFTER code;

-- 2. Adiciona a coluna de relacionamento com a empresa
ALTER TABLE tb_buildings
    ADD COLUMN company_id BIGINT NOT NULL AFTER address;

-- 3. Adiciona a Chave Estrangeira (Foreign Key) vinculando à tb_companies
ALTER TABLE tb_buildings
    ADD CONSTRAINT fk_building_company
        FOREIGN KEY (company_id) REFERENCES tb_companies(id);