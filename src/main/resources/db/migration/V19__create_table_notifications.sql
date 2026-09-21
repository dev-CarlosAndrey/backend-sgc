CREATE TABLE tb_notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    service_order_id BIGINT NOT NULL,
    recipient_id BIGINT NOT NULL,
    channel VARCHAR(20) NOT NULL,          -- WHATSAPP, EMAIL
    delivery_status VARCHAR(20) NOT NULL,  -- SENT, FAILED
    attempt INT NOT NULL,                  -- nº da tentativa dentro do canal
    error_message VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_notification_service_order FOREIGN KEY (service_order_id) REFERENCES tb_service_orders (id),
    CONSTRAINT fk_notification_recipient FOREIGN KEY (recipient_id) REFERENCES tb_users (id)
);

CREATE INDEX idx_notification_service_order ON tb_notifications (service_order_id);
