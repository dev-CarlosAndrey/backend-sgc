-- Usuário administrador inicial, necessário para operar o sistema agora que os
-- endpoints de escrita em cadastros mestres exigem @PreAuthorize("hasRole('ADMIN')")
-- (sem esta semente, ninguém conseguiria criar o primeiro usuário via API).
-- Login: admin / senha: Admin@123 — trocar a senha antes de qualquer demonstração real.
INSERT INTO tb_users (name, email, password, phone, role, active)
VALUES ('admin', 'admin@sgc.local', '$2a$10$eLn2eXBDJMrHKbr82nt.h.OwGUJDG9EUGYbnnOItchzbWD6xLORPG', NULL, 'ADMIN', TRUE);
