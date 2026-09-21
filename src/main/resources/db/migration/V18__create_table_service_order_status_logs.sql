CREATE TABLE tb_service_order_status_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    service_order_id BIGINT NOT NULL,
    previous_status VARCHAR(30) NULL,     -- NULL no registro inicial (criação da OS)
    new_status VARCHAR(30) NOT NULL,
    changed_by VARCHAR(150),              -- usuário autenticado (ou SYSTEM)
    note TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_status_log_service_order FOREIGN KEY (service_order_id) REFERENCES tb_service_orders (id)
);

CREATE INDEX idx_status_log_service_order ON tb_service_order_status_logs (service_order_id);
