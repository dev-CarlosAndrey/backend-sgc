# Manual do Usuário — SGC

> Exigido pelas diretrizes de TCC da FPO para a modalidade III (MVP): "documentação técnica: elaboração de manuais de uso [...]" (`docs/apoio/Orientações de TCC para ADS -2026.pdf`). Este manual descreve o uso do sistema do ponto de vista do usuário final (painel administrativo em React + canal de WhatsApp), não a implementação — para isso, ver `DOCUMENTACAO_TECNICA_TCC.md` e `API_CONTRATO_FRONTEND.md`.

## 1. Perfis de usuário

O SGC tem três perfis de acesso, definidos no cadastro do usuário:

| Perfil | O que pode fazer |
|---|---|
| **Administrador** | Acesso total: cadastros mestres (usuários, empresas, prédios, setores, funcionários, marcas, materiais, produtos, categorias, bens), todas as ações de Ordem de Serviço (abrir, atribuir técnico, mudar status, resolver, excluir), painel de indicadores e relatórios. |
| **Técnico** | Recebe Ordens de Serviço atribuídas, atualiza o status e registra o laudo de resolução. Não acessa cadastros mestres nem relatórios. |
| **Solicitante** (qualquer funcionário/professor) | Abre Ordens de Serviço (pelo painel ou pelo WhatsApp) e acompanha o status dos próprios chamados. |

## 2. Acesso ao sistema (painel)

1. Acesse a tela de login do SGC.
2. Informe o **nome de usuário** (não é o e-mail — o login usa o campo *Nome* do cadastro) e a senha.
3. Em caso de erro "usuário ou senha incorretos" mesmo com os dados certos, confirme que está digitando o **nome de usuário**, não o e-mail.

> Ambiente de demonstração: existe um usuário administrador de inicialização (`admin` / `Admin@123`, criado pela migração `V16`). **Troque essa senha antes de qualquer uso real.**

## 3. Controle patrimonial (Administrador)

- **Cadastrar um bem**: menu de Ativos → Novo Ativo. Campos obrigatórios: tombamento (único) e nome; opcionais: categoria, setor, responsável (funcionário), modelo, número de série, data e valor de aquisição, estado de conservação e foto.
- **Consultar bens**: filtros por setor, situação, categoria, responsável ou tombamento.
- **Histórico de um bem**: cada bem mostra o histórico de Ordens de Serviço vinculadas a ele (manutenções) e o histórico de movimentações entre setores.
- **Movimentar um bem**: transferência entre setores, com motivo e data.

## 4. Abertura de chamado (Ordem de Serviço)

### 4.1 Pelo painel

Qualquer usuário autenticado pode abrir um chamado: menu de Ordens de Serviço → Nova OS. Informar prioridade, descrição do problema e, opcionalmente, o bem patrimonial relacionado — ao vincular um bem, ele é automaticamente marcado como "em manutenção".

### 4.2 Pelo WhatsApp

Envie uma mensagem para o número do SGC no seguinte formato:

```
Setor: <nome do setor>
Bem: <tombamento, opcional>
Problema: <descrição>
```

- O setor precisa existir no sistema com esse nome exato (sem busca aproximada).
- `Bem:` é opcional — se informado, precisa ser um tombamento já cadastrado.
- Não é necessário ter cadastro prévio: o sistema identifica o solicitante pelo número de telefone e, se for a primeira vez, cria automaticamente um usuário associado àquele número.
- O bot responde confirmando a abertura com o número da OS, ou explicando o que corrigir (setor/bem não encontrado, formato inválido).

### 4.3 Consultar status pelo WhatsApp

- `Status` — lista as 5 Ordens de Serviço mais recentes abertas por aquele número.
- `Status #<número>` ou `Status <número>` — detalhe de uma OS específica (só é exibida se o chamado pertencer ao número que está consultando).

## 5. Acompanhamento da Ordem de Serviço (Técnico/Administrador)

1. **Atribuir técnico** (Administrador): a OS passa para "Em andamento" automaticamente.
2. **Mudar status** (Técnico ou Administrador): atualizações intermediárias, com observação opcional.
3. **Encerrar com laudo** (Técnico ou Administrador): obrigatório preencher o laudo/solução técnica; custo do reparo é opcional. O bem vinculado volta automaticamente para "Em uso".
4. Ordens de Serviço **Resolvidas** ou **Canceladas** não aceitam nova mudança de status.

## 6. Notificações

A cada evento relevante (abertura, atribuição de técnico, mudança de status, resolução), o solicitante e o técnico envolvido recebem uma notificação automática:

1. O sistema tenta enviar primeiro pelo **WhatsApp**.
2. Se o WhatsApp falhar por qualquer motivo, o sistema envia automaticamente por **e-mail** como alternativa — o usuário não precisa fazer nada, e não há risco de "ficar sem saber" que o chamado mudou de status.

## 7. Relatórios e painel (Administrador)

- **Painel de indicadores**: total de bens, distribuição por empresa/situação, chamados ativos, custo de manutenção acumulado, bens sob controle de depreciação — todos calculados a partir de dados reais.
- **Relatórios disponíveis**: bens por categoria/situação, Ordens de Serviço por período/técnico/status, custo de manutenção × valor de aquisição (com sinalização de bens candidatos a substituição), evolução mensal de OS, tempo médio por etapa de atendimento, patrimônio com depreciação, bens críticos, resumo de movimentações e de auditoria.
- **Exportação**: cada relatório pode ser exportado em **CSV** (para planilha) ou **PDF** (para leitura/impressão), pelo botão correspondente na tela do relatório. O PDF é gerado na hora e não fica salvo em lugar nenhum — para obtê-lo de novo, basta exportar outra vez.

## 8. Dúvidas frequentes

| Situação | O que fazer |
|---|---|
| "Usuário ou senha incorretos" mesmo com dados certos | Confirme que está usando o **nome de usuário**, não o e-mail, no campo de login. |
| Mensagem de WhatsApp não abriu chamado | Confira se o texto segue exatamente o formato `Setor:`/`Problema:` (e `Bem:` se usado) e se o nome do setor está escrito igual ao cadastrado. |
| Não recebi notificação de mudança de status | Verifique a caixa de e-mail (incluindo spam) — o sistema envia por e-mail automaticamente quando o WhatsApp falha. |
| Relatório exportado em PDF sumiu | Esperado: o PDF não é armazenado pelo sistema. Exporte novamente quando precisar. |
