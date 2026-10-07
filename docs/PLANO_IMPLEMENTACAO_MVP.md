# Plano de Implementação — Sistema de Gerenciamento e Controle Patrimonial Integrado ao WhatsApp

> Documento gerado a partir do TCC "**Sistema de Gerenciamento e Controle Patrimonial Integrado ao WhatsApp**" (Carlos Andrey Bezerra Henrique, FPO, 2026). Serve como referência técnica para a implementação do MVP. Deve ficar na raiz do repositório para consulta por ferramentas de codificação assistida (Claude CLI).
>
> Status de cada item pode ser marcado com `[x]` conforme a implementação avança.

---

## 0. Status atual (atualizado em 2026-10-02 — RF15 e RF16 implementados e testados contra o banco real no mesmo dia: abertura de OS e consulta de status por mensagem de WhatsApp. Em 2026-09-28, RF14/RF17 já haviam sido validados ponta a ponta. Todos os RFs do WhatsApp estão concluídos)

Levantamento feito diretamente sobre o código em `../src/main/java/br/com/fpo/sgc` e as migrations em `src/main/resources/db/migration` (V1–V19).

- **Fase 1 (núcleo sem WhatsApp)**: implementada em grande parte, com módulos a mais do que o desenho original previa (ver seção 5.1) e algumas lacunas pontuais em relação ao RF descrito (ver seção 7).
- **Fase 2 (WhatsApp Cloud API)**: **concluída**. Transporte (RF14/RF17) validado em 2026-09-28; abertura de OS (RF15) e consulta de status (RF16) por mensagem, ambas implementadas e validadas em 2026-10-02 — ver detalhes na seção 7.4. Falta só o chatbot ganhar mais nuance de conversa (ex.: confirmar antes de abrir, múltiplas mensagens), que não é requisito do MVP.
- **Fase 3 (fallback SendGrid)**: **concluída**. `NotificationServiceImpl` tenta WhatsApp primeiro e cai para e-mail automaticamente em caso de falha (token expirado, número restrito, fora da janela de atendimento de 24h etc.), com retentativa e registro de cada tentativa em `tb_notifications`.
- **Fase 4 (relatórios/dashboard)**: indicadores do dashboard 100% reais desde 2026-09-21 (ver RF18); relatórios dedicados por categoria (RF19) e por período/técnico/status (RF20) implementados em 2026-09-21, com CSV.
- **Fase 5 (polimento para banca)**: parcial — suíte de testes cresceu para 86 testes unitários (Mockito, sem banco) cobrindo notificação, webhook do WhatsApp, histórico de status de OS e relatórios; ainda sem seed de demonstração além do usuário `admin` de bootstrap (`V16`).

> **Risco a monitorar**: `WhatsAppNotificationSender` envia mensagem de texto livre (`type: "text"`), que a Cloud API só entrega dentro da janela de atendimento de 24h desde a última mensagem do destinatário. Com o RF15 implementado, quem abre OS pelo WhatsApp já "abriu a janela" e deve receber a notificação de volta normalmente; o cenário que ainda cai no fallback de e-mail é notificar alguém que nunca escreveu para o número (ex.: um técnico atribuído que não abriu a OS) — segue válido até existir mensagem de *template* aprovada pela Meta para conversas fora da janela de 24h.

### 0.1 Divergências de nomenclatura em relação ao plano

O código foi implementado em inglês (`br.com.fpo.sgc.domain.*`), enquanto o plano original usa nomes em português. Mapeamento para referência:

| Termo do plano | Implementado como |
|---|---|
| `usuario` | `domain.user.User` (tabela `tb_users`) |
| `bem_patrimonial` | `domain.asset.Asset` (tabela `tb_assets`) |
| `ordem_servico` | `domain.serviceorder.ServiceOrder` (tabela `tb_service_orders`) |
| `localizacao` | `domain.location.building.Building` + `domain.location.sector.Sector` (tabelas `tb_buildings`, `tb_sectors`), agrupados por `domain.company.Company` |
| `categoria_bem` | Implementado em 2026-09-21 como `domain.asset.category.AssetCategory` (tabela `tb_asset_categories`, FK opcional `tb_assets.category_id`) — entidade dedicada, decidida em vez de reaproveitar `Material`/`Product`/`Brand` (que continuam como catálogo auxiliar independente, não vinculado ao `Asset` — ver nota em 7.2). Ver RF03 na seção 7.2. |
| `os_status_log` | Implementado em 2026-09-21 como `domain.serviceorder.statuslog.ServiceOrderStatusLog` (tabela `tb_service_order_status_logs`, V18). Ver RF10 na seção 7.3. |
| `baixa_patrimonial` | Não existe; o soft delete padrão (`active = false`) é o único mecanismo de "baixa" hoje. |
| `notificacao` | Implementado em 2026-09-21 como `domain.notification.Notification` (tabela `tb_notifications`, V19). Ver seção 9.1. |
| `chatbot_sessao` | Não existe. |

Módulos implementados que **não estavam no modelo de dados original** do plano e devem ser considerados no desenho final:
- `domain.employee.Employee` — cadastro de funcionários (nome, CPF, matrícula, cargo, empresa, setor), separado de `User`. Hoje não há vínculo entre `Employee` e a carga patrimonial de um `Asset`.
- `domain.asset.movement.AssetMovement` — histórico de transferência de um `Asset` entre setores (origem/destino/motivo/data), útil como parte do "histórico do bem" do RF13, mas cobre apenas movimentações físicas, não manutenções.
- `domain.company.Company` — multiempresa/multi-tenant (CNPJ, e-mail, telefone, logo), usado como agregador de `Building`.
- `common.storage.FileStorageService` — upload genérico de arquivos para `uploads/<subdir>/<uuid>.<ext>`, servido em `/uploads/**` (liberado no `SecurityConfig`). Ainda não usado para foto de `Asset` (o campo `foto_url` do plano não existe na entidade).

### 0.2 Decisão de arquitetura em avaliação (2026-09-21): WhatsApp Cloud API no lugar da Evolution API

**Motivo.** Desde o início de 2026 a Meta intensificou fortemente a detecção e o banimento de conexões não oficiais ao WhatsApp (Evolution API, Baileys, WPPConnect, Z-API e afins), que funcionam simulando o WhatsApp Web via engenharia reversa. Relatos recentes (blogs técnicos brasileiros, set/2026) apontam suspensão de 40–60% das contas usando essas APIs alternativas logo no início de 2026, em até 48h em muitos casos, sem processo de recurso e independente de a ferramenta ser paga ou gratuita. Para um MVP que precisa estar de pé (e com o número de WhatsApp funcionando) no dia da defesa, esse risco foi considerado alto demais para seguir com Evolution API como estava planejado.

**Direção adotada para teste.** Substituir a Evolution API pela **WhatsApp Cloud API oficial (Meta)** como integração primária. Para desenvolvimento e para a própria demonstração em banca, os dois caminhos mais indicados — sem risco de banimento e sem custo — são:
- **Número de teste da própria Meta**: gratuito, permite enviar/receber mensagens de até 5 números de destino verificados, sem verificação de empresa.
- **Twilio Sandbox for WhatsApp**: ambiente de teste de um provedor oficial (BSP), configurável em minutos, sem processo de aprovação da Meta.

**Custo (referência).** Desde jul/2025 a cobrança da Cloud API é por mensagem de *template* entregue. Mensagens de resposta dentro da janela de atendimento de 24h (o caso de uso principal aqui: professor manda mensagem → bot responde) são gratuitas; só mensagens iniciadas pelo sistema fora dessa janela (ex.: notificação proativa depois de 24h de inatividade) usam template pago — volume irrelevante para uma demonstração de TCC.

**Impacto na arquitetura.** Mínimo. O desenho já previsto (webhook recebendo eventos, máquina de estados do chatbot, `NotificationService`) não muda; troca-se apenas o cliente HTTP dentro de `integration/whatsapp` e as variáveis de ambiente (ver seção 9.1). As seções 6, 7.4 e 9 deste documento já foram atualizadas para refletir a Cloud API como opção primária a ser testada.

**Status.** Decisão **totalmente validada na prática**: RF14/RF17 em 2026-09-28 (envio, webhook e recebimento com mensagens reais) e RF15/RF16 em 2026-10-02 (abertura de OS e consulta de status por mensagem, testadas contra o banco real — ver seção 7.4). Os quatro requisitos funcionais do WhatsApp estão concluídos. **A escrita oficial do TCC ainda não foi alterada** (ver seção 12) — a revisão do texto deve ser feita com cuidado junto ao usuário, não automaticamente a partir desta nota.

## 1. Contexto

Instituição de ensino privada (Faculdade Princesa do Oeste) sem sistema centralizado de controle patrimonial e de manutenção. Solicitações de manutenção hoje são informais (papel, verbal, mensagens avulsas), causando atrasos, perda de informação e ausência de rastreabilidade. O TCC propõe um sistema que:

1. Centraliza o cadastro e controle dos bens patrimoniais.
2. Formaliza e rastreia manutenções por meio de **Ordens de Serviço (OS)**.
3. Permite que professores/funcionários abram e acompanhem chamados **via WhatsApp** (chatbot), além do painel administrativo.

A pesquisa de campo (14 questões, 13 respondentes) confirmou alta aceitação do WhatsApp como canal preferido para abertura de chamados e recebimento de notificações de andamento — esse é o diferencial central do produto, não um recurso secundário.

## 2. Objetivo do MVP

Entregar um sistema funcional, demonstrável em banca, que cubra o ciclo completo: **cadastro de patrimônio → abertura de OS (painel ou WhatsApp) → execução → notificação → conclusão → histórico/relatório**. Robustez de infraestrutura (ex.: fallback de notificação) é tratada como diferencial de qualidade, não bloqueador do MVP.

> **Onde estamos**: o ciclo "cadastro de patrimônio → abertura de OS (painel **ou WhatsApp**) → execução → notificação → conclusão → histórico/relatório (painel **ou WhatsApp**)" funciona de ponta a ponta, com notificação por e-mail (e por WhatsApp quando dentro da janela de 24h — ver seção 0). O MVP não tem mais nenhum elo faltando no ciclo do WhatsApp; o que resta é polimento (Fase 5).

## 3. Glossário de domínio (extraído do referencial teórico)

| Termo | Definição |
|---|---|
| **Tombamento** | Identificação individualizada do bem por número sequencial (etiqueta/plaqueta). Todo bem patrimonial cadastrado precisa de um código de tombamento único. |
| **Carga Patrimonial** | Responsabilidade formal de um usuário (consignatário) pela guarda e uso de um bem. |
| **Baixa/Desfazimento** | Retirada contábil/física de um bem do acervo (alienação, obsolescência, dano, extravio). |
| **Material Permanente** vs **Material de Consumo** | Só material permanente (durabilidade > 2 anos) entra no controle patrimonial. |
| **Ordem de Serviço (OS)** | Instrução formal (física ou eletrônica) que registra uma necessidade de manutenção: código, descrição, data de emissão, data de execução, responsável pela solicitação. |

Esses conceitos devem estar refletidos no modelo de dados e na nomenclatura do código (não usar termos genéricos como "item" ou "ticket" sem mapear para os termos do domínio). **Nota**: a implementação atual usa `Asset`/`ServiceOrder` (nomenclatura em inglês) em vez dos termos em português citados aqui — ver seção 0.1.

## 4. Perfis de usuário

- **Administrador**: gestão total (usuários, bens, categorias, localizações, relatórios).
- **Técnico de manutenção**: recebe OS atribuídas, atualiza status, registra laudo/observações.
- **Solicitante** (professor/funcionário): abre OS (painel ou WhatsApp), acompanha status dos próprios chamados.

Implementado como enum `Role` (`ADMIN`, `TECHNICIAN`, `USER`) em `User`, embutido como claim no JWT. Restrição de acesso por papel a nível de endpoint implementada em 2026-09-21 via `@PreAuthorize` (ver RF01 na seção 7.1) — `UserDetailsServiceCustom` já concedia a authority `ROLE_<role>`, bastou habilitar `@EnableMethodSecurity` em `SecurityConfig` e anotar os métodos dos controllers.

## 5. Modelo de dados (entidades sugeridas)

Tabela original do plano, mantida como referência de desenho. Ver seção 0.1 para o mapeamento com o que foi de fato implementado.

| Entidade | Campos principais |
|---|---|
| `usuario` | id, nome, email, senha_hash, telefone_whatsapp, perfil (`ADMIN`, `TECNICO`, `SOLICITANTE`), ativo |
| `categoria_bem` | id, nome, descricao (ex.: Informática, Mobiliário, Elétrico, Predial) |
| `localizacao` | id, nome, bloco/sala, descricao |
| `bem_patrimonial` | id, codigo_tombamento, descricao, categoria_id, localizacao_id, responsavel_id (carga patrimonial), data_aquisicao, valor_aquisicao, estado_conservacao, situacao (`ATIVO`, `EM_MANUTENCAO`, `BAIXADO`), foto_url, criado_em |
| `baixa_patrimonial` | id, bem_id, motivo, data_baixa, responsavel_id, observacao *(pode ficar para pós-MVP)* |
| `ordem_servico` | id, codigo_os, bem_id (opcional), solicitante_id, tecnico_id (opcional), descricao_problema, prioridade (`BAIXA`, `MEDIA`, `ALTA`, `URGENTE`), status (`ABERTA`, `EM_ANDAMENTO`, `AGUARDANDO_PECA`, `CONCLUIDA`, `CANCELADA`), canal_abertura (`WHATSAPP`, `PAINEL`), data_abertura, data_atribuicao, data_conclusao, laudo_tecnico |
| `os_status_log` | id, os_id, status_anterior, status_novo, alterado_por, data_alteracao, observacao (histórico de manutenção do bem é derivado daqui + `ordem_servico`) |
| `chatbot_sessao` | id, numero_whatsapp, usuario_id (nullable até identificar), estado_atual (máquina de estados da conversa), contexto_json, atualizado_em |
| `notificacao` | id, os_id, usuario_id, canal (`WHATSAPP`, `EMAIL`), status_envio (`ENVIADO`, `FALHOU`), tentativa, criado_em (auditoria do fallback, ver seção 9) |

Diagrama entidade-relacionamento deve ser produzido (Mermaid ou DrawSQL, como citado no TCC) antes de gerar as migrations. **Pendente** — não há diagrama no repositório; as migrations V1–V12 já divergem do modelo acima e um ER atualizado do estado real ajudaria a planejar as próximas migrations (V13+) sem retrabalho.

### 5.1 Entidades efetivamente implementadas (V1–V12)

| Migration | Entidade JPA | Observação |
|---|---|---|
| V1 | `User` | `Role` = `ADMIN`/`TECHNICIAN`/`USER`; sem `telefone_whatsapp` dedicado, usa `phone` genérico. |
| V2 | `Building`, `Sector` | `Sector` tem `manager` (`Employee`), adicionado na V9. |
| V3 | `Asset` | Sem categoria, sem responsável (carga patrimonial), sem data/valor de aquisição, sem `foto_url`. `AssetStatus` = `AVAILABLE`/`IN_USE`/`IN_MAINTENANCE`/`DISCARDED`. |
| V4 | `ServiceOrder` | `ServiceOrderStatus` = `OPEN`/`IN_PROGRESS`/`RESOLVED`/`CANCELLED`; `ServiceOrderPriority` = `LOW`/`MEDIUM`/`HIGH`/`URGENT`. Sem `canal_abertura`. |
| V5 | `AuditLog` | Genérico (entityName/entityId/action/performedBy/details), usado por todos os services de escrita. |
| V6 | `Brand` | Catálogo de marcas. |
| V7 | `Company` | Multiempresa. |
| V8 | `Building` (alter) | Adiciona `address` e vínculo com `Company`. |
| V9 | `Employee`, `Sector` (alter) | Cadastro de funcionários + `manager` em `Sector`. |
| V10 | `Material` | Catálogo de materiais (nome + categoria). |
| V11 | `Product` | Vincula `Brand` + `Material`. |
| V12 | `AssetMovement` | Transferência de `Asset` entre setores (origem/destino/motivo/data). |
| V13 | `ServiceOrder` (alter) | Adiciona `channel` (`ServiceOrderChannel`: `PANEL`/`WHATSAPP`), sempre `PANEL` até o webhook do WhatsApp (RF14) existir. |
| V14 | `AssetCategory` (nova) + `Asset` (alter) | Cria `tb_asset_categories` (id, name único, description, active) e adiciona FK opcional `category_id` em `tb_assets`. |
| V15 | `Asset` (alter) | Adiciona `responsible_employee_id` (FK opcional para `tb_employees`, carga patrimonial), `acquisition_date`, `acquisition_value`, `conservation_state` (`AssetConservationState`: `NEW`/`GOOD`/`REGULAR`/`BAD`/`UNSERVICEABLE`) e `photo_url`. |
| V16 | `User` (seed) | Insere o usuário administrador de bootstrap (`admin` / `Admin@123`, `role=ADMIN`), necessário porque os endpoints de escrita de `UserController` passaram a exigir `ADMIN` (RF01). Trocar a senha antes de uso real. |
| V17 | `ServiceOrder` (alter) | Adiciona `cost` (opcional, preenchido ao resolver a OS), usado para calcular `maintenanceCost` real no dashboard (RF18). |
| V18 | `ServiceOrderStatusLog` (nova) | Cria `tb_service_order_status_logs` (OS, status anterior/novo, `changed_by`, `note`), alimentada por todas as mudanças de status da OS (RF10). |
| V19 | `Notification` (nova) | Cria `tb_notifications` (OS, destinatário, canal `WHATSAPP`/`EMAIL`, `delivery_status` `SENT`/`FAILED`, nº da tentativa, erro), um registro por tentativa de envio (seção 9.1). |

## 6. Arquitetura

Seguir o que já foi definido no TCC (não são sugestões alternativas, são a arquitetura descrita no trabalho):

- **Padrão**: MVC, backend e frontend desacoplados, comunicação via API REST/JSON.
- **Backend**: Java + Spring Boot, Spring Data JPA (camada Model), Controllers para painel administrativo **e** para o webhook do chatbot do WhatsApp.
- **Banco de dados**: relacional (MySQL), modelado previamente (DrawSQL).
- **Frontend**: React + Vite, consumindo a API REST.
- **Design/documentação de apoio**: Figma (protótipos de tela), Mermaid (fluxos do chatbot e arquitetura).

> **Status**: backend em Spring Boot 4.1/Java 21 seguindo o padrão MVC com camadas entity/repository/service+impl/controller/dto/mapper (ver `../CLAUDE.md`), banco MySQL com Flyway — de acordo com o plano. **O webhook do WhatsApp existe** (`domain.whatsapp.WhatsAppWebhookController`, recebe e valida eventos da Meta) **e o envio existe** (`domain.notification.WhatsAppNotificationSender`), mas nenhum dos dois aciona a máquina de estados do chatbot — o webhook só loga a mensagem recebida, não abre nem consulta OS. Frontend React/Vite não faz parte deste repositório (backend apenas).

Sugestão de organização de pastas (ajustar ao gerador de projeto usado, ex. Spring Initializr):

```
backend/
  src/main/java/.../controller/      # REST controllers (painel + webhook whatsapp)
  src/main/java/.../service/         # regras de negócio (OS, patrimônio, notificação)
  src/main/java/.../repository/      # Spring Data JPA
  src/main/java/.../model/           # entidades JPA
  src/main/java/.../dto/
  src/main/java/.../integration/whatsapp/   # cliente WhatsApp Cloud API (Meta) — testado via Twilio Sandbox / número de teste (ver seção 0.2)
  src/main/java/.../integration/email/      # cliente SendGrid (fallback)
  src/main/java/.../chatbot/         # máquina de estados da conversa
  src/main/resources/application.yml

frontend/
  src/pages/
  src/components/
  src/services/api.ts
  src/routes/
```

> A organização real (`br.com.fpo.sgc.domain.<entidade>` com `entity/repository/service+impl/controller/dto/mapper` por pacote) segue o espírito da sugestão acima, mas por **domínio** em vez de por **camada técnica**. Os pacotes sugeridos `integration/whatsapp`/`integration/email`/`chatbot` nunca existiram com esses nomes — o equivalente real é `domain.whatsapp` (webhook, parsing de comando de abertura/status, intake e consulta) e `domain.notification` (envio, WhatsApp e e-mail), todos implementados. Não há uma tabela/máquina de estados `chatbot_sessao` — decisão deliberada (ver RF15/RF16 na seção 7.4): cada mensagem é interpretada sozinha, sem sessão de conversa.

## 7. Requisitos funcionais (RF)

Rastreabilidade direta com os objetivos específicos do TCC (indicado entre colchetes).

### 7.1 Autenticação e usuários
- [x] RF01 — Login com e-mail/senha (JWT). Implementado em `AuthController` + `TokenService` (auth0 java-jwt, HS256, expiração 8h, issuer `sgc-api`, claim `role`). Controle de acesso por perfil implementado em 2026-09-21 via `@PreAuthorize` (`@EnableMethodSecurity` em `SecurityConfig`), política acordada com o usuário: escritas (create/update/delete/upload) em cadastros mestres (usuários, empresas, prédios, setores, funcionários, marcas, materiais, produtos, categorias de bem, ativos, movimentações de ativo) exigem `ADMIN`; leituras ficam abertas a qualquer autenticado; em Ordem de Serviço, abrir chamado é permitido a qualquer autenticado (`ADMIN`/`TECHNICIAN`/`USER`), atribuir técnico e excluir exigem `ADMIN`, resolver exige `ADMIN` ou `TECHNICIAN`; painel (`Dashboard`) restrito a `ADMIN`. Testado ponta a ponta com a aplicação rodando (login, 403 para ação fora do papel, 200/201 para ação permitida). Como os endpoints de cadastro de usuário agora exigem `ADMIN`, foi necessário criar a migration `V16__seed_bootstrap_admin_user.sql` com um usuário administrador inicial (login `admin` / senha `Admin@123`) para não travar o bootstrap do sistema — **trocar essa senha antes de qualquer demonstração real ou deploy**.
- [~] RF02 — Cadastro/edição/inativação de usuários implementado em `UserController`/`UserServiceImpl` (CRUD + soft delete via `active`). Campo `phone` existe, mas **não é obrigatório nem validado especificamente como WhatsApp** (sem máscara/validação de formato, sem flag de "WhatsApp habilitado").

### 7.2 Controle patrimonial `[cadastro e controle dos bens]`
- [x] RF03 — CRUD de categorias de bens. Implementado em 2026-09-21: entidade dedicada `AssetCategory` (`domain.asset.category`, tabela `tb_asset_categories`, migration V14), com CRUD completo (`AssetCategoryController`/`AssetCategoryServiceImpl`, soft delete, auditoria) em `/api/v1/asset-categories`, e FK opcional `category_id` em `Asset` (`AssetRequestDTO.categoryId`, resolvida e validada em `AssetServiceImpl`). Decisão tomada com o usuário: manter `AssetCategory` separada de `Material`/`Product`/`Brand`, que continuam sendo um catálogo auxiliar independente (não é taxonomia do bem) — ver seção 0.1.
- [x] RF04 — CRUD de localizações. Implementado como `Company` → `Building` → `Sector` (`CompanyController`, `BuildingController`, `SectorController`), mais completo que o `localizacao` simples do plano.
- [x] RF05 — CRUD de bens patrimoniais implementado em `AssetController`/`AssetServiceImpl`, com `tagNumber` (tombamento, único), nome, descrição, modelo, número de série, `status` (`AVAILABLE`/`IN_USE`/`IN_MAINTENANCE`/`DISCARDED`), `sector`, `category` (RF03, 2026-09-21) e, desde 2026-09-21 (migration V15): `responsible` (carga patrimonial, FK opcional para `Employee`), `acquisitionDate`, `acquisitionValue`, `conservationState` (`AssetConservationState`) e `photoUrl`, este último via upload dedicado `POST /api/v1/assets/{id}/photo` (multipart, reaproveitando `FileStorageService`, mesmo padrão do logo de `Company`). Todos os campos novos são opcionais — bens já cadastrados continuam válidos sem eles.
- [x] RF06 — Consulta/filtro de bens: implementado por setor (`GET /api/v1/assets/sector/{sectorId}`), por tombamento (`GET /api/v1/assets/tag/{tagNumber}`), por situação (`GET /api/v1/assets/status/{status}`), por categoria (`GET /api/v1/assets/category/{categoryId}`) e por responsável (`GET /api/v1/assets/responsible/{responsibleId}`) — os três últimos adicionados em 2026-09-21, além de listagem paginada geral.
- [ ] RF07 — Baixa/desfazimento de bem com motivo e responsável. Não implementado; hoje só existe soft delete genérico (`active = false`, sem motivo/observação). Confirmado como pós-MVP no próprio plano.

### 7.3 Ordens de serviço `[registro e acompanhamento de OS]`
- [x] RF08 — Abertura de OS pelo painel implementada (`POST /api/v1/service-orders`), vinculada ou não a um `Asset` (campo opcional), com `priority` e `description`. Ao vincular um `Asset`, o service já marca o ativo como `IN_MAINTENANCE` automaticamente. Campo `canal_abertura` implementado em 2026-09-21 como `ServiceOrderChannel` (`PANEL`/`WHATSAPP`, coluna `channel`, migration V13) — `create(dto)` continua fixando `PANEL` (painel administrativo); desde 2026-10-02 existe um overload `create(dto, channel)`, usado pelo RF15 para marcar `WHATSAPP` nas OS abertas por mensagem (ver 7.4).
- [x] RF09 — Atribuição de OS a um técnico implementada (`PATCH /api/v1/service-orders/{id}/assign-technician/{technicianId}`), muda status para `IN_PROGRESS` e grava `AuditLog`.
- [x] RF10 — Atualização de status da OS. Implementado em 2026-09-21: `PATCH /api/v1/service-orders/{id}/status` (corpo `{status, note}`, `ADMIN`/`TECHNICIAN`) para `IN_PROGRESS` (exige técnico atribuído) e `CANCELLED` (libera o `Asset` de volta para `IN_USE`, seta `closedAt`); `RESOLVED` continua exclusivo do `/resolve` (exige laudo) e OS `RESOLVED`/`CANCELLED` não aceitam nova mudança. Todas as mudanças de status (criação, `assignTechnician`, `resolveOrder` e `changeStatus`) gravam um registro estruturado em `tb_service_order_status_logs` (V18: `previous_status`, `new_status`, `changed_by`, `note`), consultável em `GET /api/v1/service-orders/{id}/status-history` (paginado). OS criadas antes da V18 não têm histórico retroativo.
- [x] RF11 — Encerramento de OS com laudo técnico implementado (`PATCH /api/v1/service-orders/{id}/resolve`, corpo = `solutionDetails`), seta `closedAt`, volta o `Asset` vinculado para `IN_USE` e impede resolver uma OS já `RESOLVED`. Desde 2026-09-21, aceita também `?cost=` opcional (query param, retrocompatível — clientes que não enviam continuam funcionando), para registrar o custo do reparo no momento em que ele é conhecido (RF18, campo `cost` novo em `ServiceOrder`, migration V17). Desde 2026-09-21 também rejeita (400) a resolução de OS `CANCELLED`, que antes passava.
- [x] RF12 — Consulta de OS: listagem paginada geral, por status (`GET /api/v1/service-orders/status/{status}`), por solicitante (`GET /api/v1/service-orders/requester/{requesterId}`), por técnico (`GET /api/v1/service-orders/technician/{technicianId}`) e por bem (`GET /api/v1/service-orders/asset/{assetId}`, RF13) implementados em 2026-09-21. Filtro por período implementado em 2026-09-21: `GET /api/v1/service-orders/period?from=yyyy-MM-dd&to=yyyy-MM-dd&field=OPENED|CLOSED` (datas inclusivas no fuso `America/Fortaleza`, ao menos uma das duas obrigatória, `field` padrão `OPENED` = data de abertura; `CLOSED` filtra pela data de conclusão/cancelamento). Parâmetros com tipo inválido (data mal formatada, enum inexistente) passaram a responder 400 (`Parâmetro Inválido`) no `GlobalExceptionHandler`, em vez do 403 vazio anterior.
- [x] RF13 — Histórico de manutenção por bem (todas as OS vinculadas a ele). Implementado em 2026-09-21: `findByAssetIdAndActiveTrue` em `ServiceOrderRepository`, método `findByAssetId` em `ServiceOrderService`/`ServiceOrderServiceImpl`, endpoint `GET /api/v1/service-orders/asset/{assetId}` (paginado) em `ServiceOrderController`. `AssetMovement` (V12) continua cobrindo apenas histórico de transferência entre setores, agora complementado por este endpoint para o histórico de manutenção/OS.

### 7.4 Integração com WhatsApp `[integração para envio de solicitações e notificações]`

> Ver seção 0.2: a integração mudou de Evolution API para **WhatsApp Cloud API (Meta)**, validada em 2026-09-28. Os RFs abaixo descrevem o desenho para a Cloud API.

- [x] RF14 — Recebimento de mensagens via webhook do WhatsApp Cloud API (Meta). Implementado: `WhatsAppWebhookController` (`GET /api/v1/whatsapp/webhook`, liberado no `SecurityConfig` já que a Meta não envia JWT) responde ao desafio de verificação (`hub.verify_token` contra `whatsapp.cloud-api.webhook-verify-token`) e `POST` recebe os eventos, validando a assinatura `X-Hub-Signature-256` via `WhatsAppSignatureVerifier` (HMAC-SHA256, comparação em tempo constante, `whatsapp.cloud-api.app-secret`). Faz o parsing de `messages` (mensagem recebida) e `statuses` (atualização de entrega) e loga. Validado em 2026-09-28 com mensagem real recebida do WhatsApp pessoal do desenvolvedor. Coberto por `WhatsAppWebhookControllerTest`, `WhatsAppSignatureVerifierTest` e `WhatsAppWebhookPayloadTest`.
- [x] RF15 — Abertura de chamado por mensagem de WhatsApp. Implementado em 2026-10-02, decisões de produto definidas com o usuário: **qualquer pessoa pode abrir chamado** (sem exigir cadastro prévio) e **setor/bem são casados por nome/tombamento exatos** (sem busca aproximada nem fluxo de várias mensagens — prioridade era manter o uso simples). Fluxo (não é máquina de estados/sessão — é um parser de uma mensagem só, decisão deliberada para não depender de `chatbot_sessao`):
  - `WhatsAppServiceOrderCommand.parse()` reconhece o formato `Setor:`/`Bem:` (opcional)/`Problema:` em qualquer lugar do texto (`Bem:` opcional). Mensagem fora desse formato não é tratada como tentativa de comando.
  - `WhatsAppServiceOrderIntakeService` resolve o `Sector` por nome (`SectorRepository.findByNameIgnoreCaseAndActiveTrue`) e, se informado, o `Asset` por tombamento (`AssetRepository.findByTagNumberIgnoreCaseAndActiveTrue`); se qualquer um não existir, responde com erro explicando o formato correto (sem criar nada).
  - O solicitante é resolvido por telefone (`UserService.findOrCreateByPhone`, novo): se já existe um `User` com aquele telefone, reaproveita; senão cria um na hora com `role = USER`, nome do contato do WhatsApp (ou um nome genérico), e e-mail/senha sintéticos (ninguém loga no painel com essa conta).
  - A OS é criada via `ServiceOrderService.create(dto, ServiceOrderChannel.WHATSAPP)` (novo overload; `create(dto)` sem canal continua fixando `PANEL` para o painel), prioridade sempre `MEDIUM` (não perguntada), título derivado automaticamente do problema.
  - `WhatsAppAutoReplyService` orquestra tudo e responde: boas-vindas/formato (mensagem não reconhecida), erro de setor/bem (`WhatsAppMessageTemplates`), ou confirmação com o número da OS criada.
  - **Testado contra o banco real** em 2026-10-02 (não só nos testes unitários): usuário criado automaticamente, OS registrada com `channel = WHATSAPP`, disparo correto da notificação existente (RF17) para o solicitante, reaproveitamento do mesmo usuário em um segundo chamado, e os três casos de erro (setor inexistente, bem inexistente, mensagem fora do formato) confirmados sem criar usuário/OS indevidos.
  - Coberto por `WhatsAppServiceOrderCommandTest`, `WhatsAppServiceOrderIntakeServiceTest`, `UserServiceImplFindOrCreateByPhoneTest`, `ServiceOrderServiceImplChannelTest` e os testes atualizados de `WhatsAppAutoReplyServiceTest`/`WhatsAppWebhookControllerTest`.
- [x] RF16 — Consulta de status de chamado pelo próprio WhatsApp. Implementado em 2026-10-02, decisões consistentes com o RF15 (mesma mensagem só, sem sessão/múltiplas trocas):
  - `WhatsAppServiceOrderStatusQueryCommand.parse()` reconhece a mensagem inteira como `Status` (lista) ou `Status #<id>`/`Status <id>` (detalhe) — qualquer outro texto não é tratado como esse comando.
  - `WhatsAppServiceOrderStatusQueryService`: ao contrário do RF15, **não cria usuário** — telefone desconhecido só recebe "sem histórico" (`UserRepository.findByPhone`, sem fallback de criação). Sem número, lista as 5 OS mais recentes do telefone (`ServiceOrderService.findByRequesterId`); com número, busca a OS (`ServiceOrderService.findById`) e só mostra o detalhe se o `requester` bater com o telefone — OS de outro solicitante responde a mesma mensagem de "não encontrada" usada para um ID inexistente (não confirma nem nega a existência de chamado de terceiros).
  - `WhatsAppAutoReplyService.handleIncomingMessage` agora tenta, em ordem: comando de abertura (RF15) → comando de status (RF16) → boas-vindas.
  - **Bug pego e corrigido só pelo teste contra o banco real** (não pelos testes unitários com mock): a primeira versão tinha `@Transactional(readOnly = true)` no método orquestrador de `WhatsAppServiceOrderStatusQueryService`, que capturava `EntityNotFoundException` de uma chamada a `serviceOrderService.findById` (outro bean, também `@Transactional`, propagação REQUIRED). O Spring já marcava essa transação participante como rollback-only antes da exceção chegar ao `catch`; mesmo sem propagar, o commit do método orquestrador falhava depois com `UnexpectedRollbackException`. Corrigido removendo o `@Transactional` do orquestrador (cada chamada interna já gerencia a própria transação; não havia necessidade de atomicidade entre elas).
  - Coberto por `WhatsAppServiceOrderStatusQueryCommandTest`, `WhatsAppServiceOrderStatusQueryServiceTest` e os casos novos em `WhatsAppAutoReplyServiceTest`; validado de ponta a ponta contra o banco real, incluindo o caso do bug acima (consulta de OS inexistente) depois de corrigido.
- [x] RF17 — Notificação automática ao solicitante e ao técnico via WhatsApp. Implementado: `WhatsAppNotificationSender` (`domain.notification`) envia mensagem de texto livre via `POST /{phone-number-id}/messages` da Graph API; `NotificationServiceImpl` tenta WhatsApp primeiro nos eventos da OS (criação, atribuição, mudança de status, resolução) e cai automaticamente para e-mail (SendGrid) em caso de falha, com retentativa e registro de cada tentativa em `tb_notifications`. Validado em 2026-09-28 com envio real. **Limitação conhecida**: como é mensagem de texto livre (não *template*), só é entregue dentro da janela de atendimento de 24h da Cloud API — notificações proativas para quem nunca escreveu ao número comercial tendem a cair no fallback de e-mail até existir RF15 (chamado aberto pelo próprio WhatsApp) ou um template de mensagem aprovado pela Meta.

### 7.5 Relatórios e painel `[informações para tomada de decisão]`
- [x] RF18 — Dashboard implementado em `DashboardController`/`DashboardServiceImpl` (`GET /api/v1/dashboard?companyId=`, restrito a `ADMIN`), com todos os indicadores calculados a partir de dados reais desde 2026-09-21: `totalAssets`, `assetsByCompany` e contagem de ativos por `status` (via `AssetRepository`); `activeTickets` = contagem de `ServiceOrder` ativas com status diferente de `RESOLVED`/`CANCELLED` (`ServiceOrderRepository.countActiveByCompanyId`); `maintenanceCost` = soma do novo campo `ServiceOrder.cost` (só preenchido ao resolver a OS, via RF11) das ordens resolvidas (`sumMaintenanceCostByCompanyId`); `depreciatingAssets` = contagem de ativos com `acquisitionDate` e `acquisitionValue` preenchidos (`countDepreciatingByCompanyId`) — aproximação honesta de "bens com controle de depreciação ativo", já que o sistema ainda não modela vida útil/valor residual para calcular depreciação de fato. Testado ponta a ponta com a aplicação rodando. "Tempo médio de atendimento" e "bens com mais manutenções" estão no relatório de OS (RF20), não no dashboard. Correção em 2026-09-21: `assetsByStatus` passou a respeitar o filtro `companyId` (antes contava sempre todas as empresas) e sempre traz as 4 situações.
- [x] RF19 — Relatório de bens por categoria/situação. Implementado em 2026-09-21: `GET /api/v1/reports/assets?companyId=` (`ADMIN`) devolve total, totais por situação e, por categoria, o total e a contagem por situação (todas as situações, zeradas inclusive; bens sem categoria agrupados como "Sem categoria"), com exportação CSV em `GET /api/v1/reports/assets/export`. O dashboard (RF18) **não** ganhou agregação por categoria — o relatório dedicado cobre isso.
- [x] RF20 — Relatório de OS por período/técnico/status. Implementado em 2026-09-21: `GET /api/v1/reports/service-orders` (`ADMIN`; filtros opcionais `from`/`to`/`field`(`OPENED`|`CLOSED`)/`technicianId`/`status`/`companyId`) devolve total, resolvidas, custo total, **tempo médio de atendimento** (horas entre abertura e conclusão das OS `RESOLVED`), contagem por status e prioridade, quebra por técnico (incluindo "Sem técnico") e os **5 bens com mais manutenções**; `GET /api/v1/reports/service-orders/export` exporta a lista do filtro em CSV (`;`, UTF-8 com BOM, datas e status em português, proteção contra CSV injection). Agregação feita em memória sobre as OS do filtro (`findForReport`, com fetch join) — adequado à escala da instituição, a revisar se o volume crescer.
- [x] Relatório extra — custo de manutenção × valor do bem. Implementado em 2026-09-21: `GET /api/v1/reports/maintenance-cost` (`ADMIN`; filtros `from`/`to`/`field`(`CLOSED` padrão)/`companyId`/`threshold` em %, padrão 50) agrega, por bem, o custo das OS `RESOLVED` e o compara ao `acquisitionValue`, sinalizando bens candidatos a substituição (`exceedsThreshold`); CSV em `/maintenance-cost/export`. Agregação em memória sobre `findForReport`, sem consulta JPQL nova.
- [x] Relatórios de análise (para o TCC) — implementados em 2026-09-21, todos `ADMIN` e com CSV: evolução mensal de OS (`/reports/service-orders/monthly`), tempo por etapa/gargalo a partir do `ServiceOrderStatusLog` (`/service-orders/stage-times`), patrimônio com depreciação linear por categoria/setor (`/assets/patrimony`; vida útil única e parametrizável, sem valor residual — o sistema não modela vida útil por categoria), bens críticos (`/assets/critical`), resumo de movimentações (`/movements`) e de auditoria (`/audit`). Serviços separados de `ReportServiceImpl`: `ServiceOrderInsightsService`, `AssetInsightsService`, `TraceabilityReportService`. Novas consultas JPQL em `ServiceOrderStatusLogRepository`, `AssetRepository`, `AssetMovementRepository` e `AuditLogRepository`; a sintaxe foi validada pelo `contextLoads`, mas ainda **não** foram exercitadas com dados reais.
- [x] **Exportação em PDF** (implementada em 2026-10-07, para os 9 relatórios acima): `GET .../export/pdf` ao lado de cada `GET .../export` (CSV) já existente. Decisão de produto explícita do usuário: o PDF é **gerado inteiramente em memória e nunca persistido** — nada em disco, nada no banco, nada em storage externo; a única cópia é o corpo da resposta HTTP, e para obter o arquivo de novo o cliente chama o endpoint de novo.
  - `common.util.PdfWriter` (novo, usa a biblioteca `OpenPDF`): tabela com cabeçalho destacado, título do relatório e timestamp de geração; largura de cada coluna calculada pelo maior conteúdo (cabeçalho ou célula), evitando que colunas de texto longo (ex.: "Título", "Setor") fiquem comprimidas demais; paisagem automática para relatórios com muitas colunas (ex.: OS, 12 colunas).
  - `common.util.TableData` (novo, record `header`/`rows`): cada `*ServiceImpl` foi refatorado para montar a tabela uma vez só e alimentar tanto o CSV quanto o PDF a partir dela — sem duplicar a lógica de formatação entre os dois formatos.
  - Relatório de auditoria em PDF usa um limite bem menor que o CSV (300 eventos vs. 10.000) — PDF é para leitura/impressão, não para dump de dados.
  - Testado contra a aplicação real: PDF baixado e inspecionado visualmente para o relatório de OS (paisagem, 12 colunas) e o de bens por categoria (retrato) — ambos legíveis, sem overflow de texto.
  - Coberto por `PdfWriterTest` e um teste por relatório (`*Pdf...` nos 4 arquivos de teste de relatório), verificando a assinatura `%PDF` do arquivo gerado.

## 8. Requisitos não funcionais

- [x] Senhas com hash (bcrypt) — `BCryptPasswordEncoder` em `SecurityConfig`. Autenticação via JWT — `TokenService`/`SecurityFilter`.
- [~] Validação de entrada — Bean Validation (`@Valid` + `jakarta.validation`) aplicada nos `RequestDTO` dos módulos com DTO (assets, service orders, users, etc.). Não auditado item a item se toda regra de negócio relevante tem constraint correspondente.
- [x] Logs de auditoria — implementado para ações administrativas via `AuditLogService`/`AuditLog` (create/update/delete cobertos nos services que seguem a convenção do `../CLAUDE.md`). Auditoria de tentativas de notificação (sucesso/falha por canal, nº da tentativa, erro) também existe, em `tb_notifications` (V19), separada do `AuditLog` genérico.
- [~] LGPD — inativação de usuário suportada (`active = false` em soft deletes, convenção geral do projeto). Não há avaliação formal de minimização de dados pessoais coletados nem política de retenção documentada.
- [ ] Interface responsiva — fora do escopo deste repositório (backend apenas; não há frontend neste projeto).
- [x] Webhook do WhatsApp responder rápido — `WhatsAppWebhookController.receive()` só valida assinatura e faz parsing (sem chamada de rede síncrona); todo o processamento de negócio (RF15, resposta automática) roda em `WhatsAppAutoReplyService.handleIncomingMessage()`, `@Async`.

## 9. Diferencial (plus): fallback de notificação via SendGrid quando o canal WhatsApp falhar

Mesmo usando a API oficial (Cloud API), o canal WhatsApp pode falhar por motivos pontuais: token de acesso expirado, número temporariamente restrito por violação de política, limite de mensagens (`messaging limit tier`) excedido, indisponibilidade passageira da Graph API, ou atraso na aprovação de um template novo. Como as notificações são parte central da proposta do TCC, o sistema não deve depender de um único canal. Este é um requisito de robustez a ser implementado após o fluxo principal de WhatsApp estar funcionando — o raciocínio do fallback continua válido mesmo com a troca da Evolution API pela Cloud API (ver seção 0.2); muda apenas a natureza do risco que ele mitiga (antes: sessão QR instável; agora: falhas pontuais de uma API oficial, ainda assim reais).

> **Status: concluída.** `NotificationSender` (interface), `NotificationService`/`NotificationServiceImpl`, `WhatsAppNotificationSender`, `EmailNotificationSender` e a tabela `tb_notifications` (V19) existem e estão em uso. As variáveis antigas da Evolution API (`EVOLUTION_API_URL`/`EVOLUTION_API_KEY`/`EVOLUTION_INSTANCE_NAME`) já foram removidas do `application.yml`, substituídas pelas variáveis da Cloud API listadas em 9.1. O único item pendente da seção é o endpoint de health-check da integração (badge "WhatsApp: operacional/degradado" no painel) — nunca foi bloqueador do MVP, é só um diferencial de observabilidade.

### 9.1 Desenho da solução
- [x] Abstração `NotificationSender` — interface criada em 2026-09-21 (`domain.notification`: `channel()`/`canSend()`/`send()`). `EmailNotificationSender` (SendGrid, `RestClient` sobre `POST /v3/mail/send`) e `WhatsAppNotificationSender` (Cloud API, `RestClient` sobre `POST /{phone-number-id}/messages`, texto livre) implementados, ambos validados com envio real (e-mail em 2026-09-21, WhatsApp em 2026-09-28). Sem as credenciais correspondentes (`SENDGRID_API_KEY`/`SENDGRID_FROM_EMAIL` ou `WHATSAPP_ACCESS_TOKEN`/`WHATSAPP_PHONE_NUMBER_ID`) o canal respectivo fica desabilitado (`canSend()` retorna `false`).
- [x] Um `NotificationService` central (`NotificationServiceImpl`, coberto por testes unitários) decide o canal: tenta WhatsApp primeiro; se a chamada falhar (erro HTTP da Graph API, timeout, token expirado, número restrito, fora da janela de 24h ou limite de mensagens excedido), aciona o fallback por e-mail via SendGrid automaticamente. Desde 2026-09-28, o erro registrado usa o corpo da resposta HTTP da API (ex.: `{"error":{"message":"Access token has expired","code":190}}`) em vez da mensagem genérica da exception, facilitando o diagnóstico.
- [ ] Cada usuário precisa ter e-mail cadastrado (RF02) para que o fallback funcione — se não houver e-mail, registrar falha e notificar apenas por WhatsApp quando disponível. (`User.email` já existe e é obrigatório/único, então este pré-requisito de dado já está atendido.)
- [x] Disparo automático nos eventos da OS (implementado em 2026-09-21): `ServiceOrderServiceImpl` publica `ServiceOrderEvent` (`CREATED`, `TECHNICIAN_ASSIGNED`, `STATUS_CHANGED`, `RESOLVED`) e `ServiceOrderNotificationListener` (`@Async` + `@TransactionalEventListener(AFTER_COMMIT)`) notifica solicitante e técnico atribuído (sem duplicar, ignorando inativos) e, na resolução (`RESOLVED`), também **quem resolveu** — o usuário autenticado que chamou `/resolve`, que pode não ser o técnico atribuído ou a OS pode nem ter técnico (o `ServiceOrderEvent` carrega `actorUserId`). Falha de notificação nunca afeta a operação da OS; rollback descarta o evento. Desde 2026-09-28, WhatsApp é o canal tentado primeiro (ver limitação da janela de 24h na seção 7.4/RF17); e-mail continua como fallback garantido.
- [x] Registrar cada tentativa de envio (canal, status, timestamp) na tabela `notificacao` (`tb_notifications`, V19), para fins de auditoria e para a discussão de resultados do TCC (mostrar taxa de sucesso por canal).
- [ ] Endpoint/health-check simples que verifica periodicamente a validade do access token e o `quality_rating`/`messaging_limit_tier` do número junto à Graph API, expondo isso no painel administrativo (ex.: badge "WhatsApp: operacional/degradado"). Único item de robustez desta seção ainda não implementado.
- [x] Templates de e-mail equivalentes às mensagens de WhatsApp (abertura de OS, atribuição, mudança de status, conclusão), incluindo o código da OS. Implementado em 2026-09-21: o `EmailNotificationSender` envia `text/plain` + `text/html` via `ServiceOrderEmailTemplate` (assunto "OS #id - título" como título; cor do cabeçalho, rótulo e barra de progresso Aberta → Em andamento → Concluída acompanham o status da OS — azul/laranja/verde, e vermelho com etapas Aberta → Cancelada; texto dinâmico escapado). O `NotificationSender.send` recebe o `ServiceOrderStatus` para isso. O texto de cada evento vem do `ServiceOrderNotificationListener`, compartilhado com o WhatsApp (que envia o mesmo texto como mensagem simples, sem HTML).
- [x] Variáveis de ambiente (substituíram as três antigas da Evolution API, já removidas do `application.yml`): `WHATSAPP_CLOUD_API_BASE_URL` (padrão `https://graph.facebook.com/v22.0`), `WHATSAPP_ACCESS_TOKEN`, `WHATSAPP_PHONE_NUMBER_ID`, `WHATSAPP_BUSINESS_ACCOUNT_ID`, `WHATSAPP_WEBHOOK_VERIFY_TOKEN`, `WHATSAPP_APP_SECRET`, além de `SENDGRID_API_KEY`, `SENDGRID_FROM_EMAIL`, `NOTIFICATION_FALLBACK_ENABLED`. Todas mapeadas em `application.yml` e configuradas nas run configurations do IntelliJ do usuário (não versionadas — ver `../CLAUDE.md`).
- [x] Retentativas com backoff simples (`notification.max-attempts`/`retry-backoff-ms`, backoff linear) antes de considerar o canal principal indisponível (evitar falso positivo por lentidão pontual).

### 9.2 Por que isso é relevante para o TCC
Reforça, na defesa, que o sistema foi pensado para continuidade operacional mesmo usando um canal gerenciado por terceiro (Meta): falhas pontuais de rate limit, token expirado ou aprovação de template são reais mesmo em API oficial. Ter um fallback documentado responde proativamente a esse ponto — e evita a pergunta mais incômoda que a banca poderia fazer com Evolution API: "o que acontece se o número for banido no meio da apresentação?" (ver seção 0.2).

## 10. Roadmap por fases

- [x] **Fase 1 — Núcleo do sistema (sem WhatsApp)**: autenticação (x), usuários com restrição de acesso por papel (x, RF01, 2026-09-21), categorias de bem (x, RF03), localizações (x), CRUD de bens completo com categoria/responsável/aquisição/estado de conservação/foto e filtros por setor/status/categoria/responsável/tombamento (x, RF03+RF05+RF06), CRUD de OS via painel com canal de abertura (x, RF08), mudança de status (x, RF10 — endpoint genérico + `os_status_log`, 2026-09-21), consultas de OS por status/solicitante/técnico/bem (x, RF12/RF13 — ver seção 7.3), histórico de transferência (x, `AssetMovement`). **Fechada** — não restam itens `~` na Fase 1 (RF12 concluído com o filtro por período).
- [x] **Fase 2 — Integração WhatsApp (Cloud API oficial / Meta — ver seção 0.2)**: webhook de recebimento (x, RF14, validado 2026-09-28), envio via Graph API (x, RF17, validado 2026-09-28), abertura de OS por mensagem (x, RF15, validado 2026-10-02), consulta de status por mensagem (x, RF16, validado 2026-10-02). **Fechada.**
- [x] **Fase 3 — Notificações + fallback SendGrid**: notificações automáticas nos eventos da OS (x), `NotificationService` com WhatsApp como canal primário e fallback de e-mail (x, ambos validados com envio real), auditoria de envios em `tb_notifications` (x). **Fechada**, exceto o health-check da integração (não bloqueador, ver seção 9.1).
- [x] **Fase 4 — Relatórios e dashboard**: indicadores do dashboard 100% reais desde 2026-09-21 (RF18 — sem mocks). Relatórios dedicados por categoria (RF19) e por período/técnico/status (RF20) implementados em 2026-09-21, com CSV; exportação em **PDF** implementada em 2026-10-07 para os 9 relatórios (JSON + CSV + PDF). **Fechada.**
- [~] **Fase 5 — Polimento para a banca**: responsividade (n/a, sem frontend no repo), dados de demonstração (seed) **ainda não existem** (só o usuário `admin` de bootstrap, `V16`, que não é massa de demonstração), revisão de segurança básica com `@PreAuthorize`/restrição por `Role` **concluída em 2026-09-21** (RF01). Testes dos fluxos críticos **evoluíram bastante**: 86 testes unitários (Mockito, sem banco) cobrindo notificação/fallback, webhook e assinatura do WhatsApp, histórico de status de OS e regras de resolução — falta apenas seed de demonstração.

## 11. Critérios de aceite do MVP

O MVP está pronto para demonstração quando:
1. [x] Um bem patrimonial pode ser cadastrado com tombamento, categoria, localização e responsável. *(completo desde 2026-09-21 — categoria via RF03, responsável/aquisição/estado de conservação/foto via RF05; todos os campos novos são opcionais, não quebram bens já cadastrados)*
2. [x] Uma OS pode ser aberta tanto pelo painel quanto por uma conversa no WhatsApp, gerando o mesmo tipo de registro no banco. *(concluído em 2026-10-02 — RF15, `channel = WHATSAPP`, testado contra o banco real)*
3. [x] O solicitante recebe uma notificação (WhatsApp ou, em caso de falha, e-mail) quando o status da OS muda. *(concluído em 2026-09-28 — WhatsApp é tentado primeiro, RF17, com fallback automático por e-mail, validado com envio real em ambos os canais)*
4. [x] É possível consultar o histórico de manutenção de um bem específico. *(implementado em 2026-09-21 — RF13, `GET /api/v1/service-orders/asset/{assetId}`)*
5. [x] O painel exibe pelo menos um relatório/indicador agregando os dados de OS. *(RF18, concluído em 2026-09-21 — todos os indicadores do dashboard, incluindo os de OS, calculados a partir de dados reais)*

**Os 5 critérios de aceite do MVP estão 100% atendidos.** Fase 1, Fase 2 (WhatsApp — RF14 a RF17, todos concluídos), Fase 3 (notificação) e Fase 4 (indicadores do dashboard) estão fechadas, incluindo a restrição de acesso por papel (RF01, `@PreAuthorize`). O único item que resta é a Fase 5 (seed de demonstração para a banca) — polimento, não requisito funcional.

## 12. Referências

Baseado no TCC "Sistema de Gerenciamento e Controle Patrimonial Integrado ao WhatsApp" (FPO, 2026), seções 1, 4, 5 e 6 (Introdução, Objetivos, Referencial Teórico e Metodologia). O item 9 (fallback SendGrid) é um acréscimo de engenharia não descrito no texto original do TCC, proposto como reforço de robustez da integração de notificações.

**Nota sobre a escrita oficial do TCC (importante):** a versão atual do texto do TCC ainda descreve a integração via **Evolution API**, conforme decidido originalmente. A mudança para **WhatsApp Cloud API**, registrada na seção 0.2 deste plano, foi motivada pela onda de banimentos de APIs não oficiais do WhatsApp observada em 2026 (contas usando Evolution API/Baileys/WPPConnect/Z-API com até 40–60% de suspensão no início de 2026, sem processo de recurso). Em **2026-09-28** a integração de envio, webhook e recebimento foi validada com mensagens reais, e em **2026-10-02** a abertura de OS e a consulta de status por mensagem (RF15/RF16) também — o critério que esta nota original exigia para liberar a atualização do texto acadêmico. Os quatro requisitos funcionais do WhatsApp (RF14–RF17) estão concluídos. **Ainda assim, não altere a escrita do TCC automaticamente com base nesta nota** — a atualização (seção 6.6 e trecho correspondente da seção 9) deve ser feita em conjunto com o usuário, respeitando o padrão ABNT e as citações já existentes.
