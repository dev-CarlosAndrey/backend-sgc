ALTER TABLE tb_assets
    ADD COLUMN responsible_employee_id BIGINT NULL,
    ADD COLUMN acquisition_date DATE NULL,
    ADD COLUMN acquisition_value DECIMAL(12,2) NULL,
    ADD COLUMN conservation_state VARCHAR(30) NULL,
    ADD COLUMN photo_url VARCHAR(255) NULL,
    ADD CONSTRAINT fk_asset_responsible FOREIGN KEY (responsible_employee_id) REFERENCES tb_employees (id);
