-- Rastreio da entrega no WhatsApp: a Cloud API responde 200 ao aceitar a mensagem, mas a falha de entrega
-- (ex.: fora da janela de 24 h) chega depois, por webhook, com o id da mensagem (wamid). Guardamos o wamid
-- e o conteúdo enviado para reenviar a notificação por e-mail nesse caso (RN08/RF24).
ALTER TABLE tb_notifications
    ADD COLUMN provider_message_id VARCHAR(128) NULL,
    ADD COLUMN subject VARCHAR(255) NULL,
    ADD COLUMN message_body VARCHAR(2000) NULL;

CREATE INDEX idx_notification_provider_message ON tb_notifications (provider_message_id);

-- Deduplicação de mensagens recebidas: a Meta pode reenviar o mesmo evento (RNF11); a chave única no id
-- da mensagem impede abrir duas OS a partir de uma só mensagem.
CREATE TABLE tb_whatsapp_inbound_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id VARCHAR(128) NOT NULL,
    from_phone VARCHAR(20) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_whatsapp_inbound_message_id UNIQUE (message_id)
);
