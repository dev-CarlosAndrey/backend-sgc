-- Avaliação do atendimento pelo solicitante, de 1 a 5, feita pelo WhatsApp depois da conclusão da OS.
-- NULL = ainda não avaliada.
ALTER TABLE tb_service_orders
    ADD COLUMN rating TINYINT NULL,
    ADD COLUMN rated_at TIMESTAMP NULL;
