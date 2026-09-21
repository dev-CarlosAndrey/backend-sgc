ALTER TABLE tb_service_orders
    ADD COLUMN channel VARCHAR(30) NOT NULL DEFAULT 'PANEL' AFTER priority;
