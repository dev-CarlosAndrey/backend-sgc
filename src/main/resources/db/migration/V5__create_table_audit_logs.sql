CREATE TABLE tb_audit_logs (
                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                               entity_name VARCHAR(100) NOT NULL,    -- Ex: "ServiceOrder", "Asset", "User"
                               entity_id BIGINT NOT NULL,            -- ID do registro alterado
                               action VARCHAR(50) NOT NULL,          -- Ex: "CREATE", "UPDATE", "DELETE", "CHANGE_STATUS"
                               performed_by VARCHAR(150),            -- Nome ou e-mail do usuário/sistema que fez a ação
                               details TEXT,                         -- Detalhes da mudança em JSON ou texto livre
                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);