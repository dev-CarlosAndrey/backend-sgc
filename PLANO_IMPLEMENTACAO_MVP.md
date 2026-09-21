# Plano de Implementação — Sistema de Gerenciamento e Controle Patrimonial Integrado ao WhatsApp

> Documento gerado a partir do TCC "**Sistema de Gerenciamento e Controle Patrimonial Integrado ao WhatsApp**" (Carlos Andrey Bezerra Henrique, FPO, 2026). Serve como referência técnica para a implementação do MVP. Deve ficar na raiz do repositório para consulta por ferramentas de codificação assistida (Claude CLI).
>
> Status de cada item pode ser marcado com `[x]` conforme a implementação avança.

---

## 0. Status atual (atualizado em 2026-09-21 — inclui decisão em avaliação na seção 0.2 e conclusão do RF01/RF03/RF05/RF06/RF08/RF12/RF13/RF18 na mesma data)

Levantamento feito diretamente sobre o código em `src/main/java/br/com/fpo/sgc` e as migrations em `src/main/resources/db/migration` (V1–V19).

- **Fase 1 (núcleo sem WhatsApp)**: implementada em grande parte, com módulos a mais do que o desenho original previa (ver seção 5.1) e algumas lacunas pontuais em relação ao RF descrito (ver seção 7).
- **Fases 2 e 3 (WhatsApp/Evolution API e fallback SendGrid)**: **não iniciadas**. Não existe nenhuma classe de integração, cliente HTTP, webhook, máquina de estados de chatbot ou `NotificationService` no código. As variáveis `EVOLUTION_API_URL`, `EVOLUTION_API_KEY`, `EVOLUTION_INSTANCE_NAME` já estão declaradas em `application.yml`, mas não são lidas por nenhum bean (confirmado também pelo `CLAUDE.md` do projeto). Não há dependência do SendGrid no `pom.xml`.
- **Fase 4 (relatórios/dashboard)**: indicadores do dashboard 100% reais desde 2026-09-21 (ver RF18); relatórios dedicados por categoria/período/técnico (RF19/RF20) ainda não existem.
- **Fase 5 (polimento para banca)**: não iniciada (sem seed de demonstração, sem suíte de testes além do `contextLoads` padrão do Spring Boot).

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

**Status.** Decisão ainda **em validação prática** — nada disso está implementado no código ainda (RF14–RF17 continuam `[ ]`). Enquanto o teste não for concluído com sucesso, **a escrita oficial do TCC continua descrevendo Evolution API** (ver seção 12); a atualização do texto acadêmico só deve ser feita depois que a integração estiver validada rodando.

## 1. Contexto

Instituição de ensino privada (Faculdade Princesa do Oeste) sem sistema centralizado de controle patrimonial e de manutenção. Solicitações de manutenção hoje são informais (papel, verbal, mensagens avulsas), causando atrasos, perda de informação e ausência de rastreabilidade. O TCC propõe um sistema que:

1. Centraliza o cadastro e controle dos bens patrimoniais.
2. Formaliza e rastreia manutenções por meio de **Ordens de Serviço (OS)**.
3. Permite que professores/funcionários abram e acompanhem chamados **via WhatsApp** (chatbot), além do painel administrativo.

A pesquisa de campo (14 questões, 13 respondentes) confirmou alta aceitação do WhatsApp como canal preferido para abertura de chamados e recebimento de notificações de andamento — esse é o diferencial central do produto, não um recurso secundário.

## 2. Objetivo do MVP

Entregar um sistema funcional, demonstrável em banca, que cubra o ciclo completo: **cadastro de patrimônio → abertura de OS (painel ou WhatsApp) → execução → notificação → conclusão → histórico/relatório**. Robustez de infraestrutura (ex.: fallback de notificação) é tratada como diferencial de qualidade, não bloqueador do MVP.

> **Onde estamos**: o ciclo "cadastro de patrimônio → abertura de OS (painel) → execução → conclusão" já funciona via API REST. Faltam "abertura via WhatsApp" e "notificação" por completo, e o "histórico/relatório" está incompleto (ver seções 7.3 e 7.5).

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

> **Status**: backend em Spring Boot 4.1/Java 21 seguindo o padrão MVC com camadas entity/repository/service+impl/controller/dto/mapper (ver `CLAUDE.md`), banco MySQL com Flyway — de acordo com o plano. **Controller/webhook do chatbot WhatsApp ainda não existe.** Frontend React/Vite não faz parte deste repositório (backend apenas).

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

> A organização real (`br.com.fpo.sgc.domain.<entidade>` com `entity/repository/service+impl/controller/dto/mapper` por pacote) segue o espírito da sugestão acima, mas por **domínio** em vez de por **camada técnica**. Os pacotes `integration/whatsapp`, `integration/email` e `chatbot` ainda não existem.

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
- [x] RF08 — Abertura de OS pelo painel implementada (`POST /api/v1/service-orders`), vinculada ou não a um `Asset` (campo opcional), com `priority` e `description`. Ao vincular um `Asset`, o service já marca o ativo como `IN_MAINTENANCE` automaticamente. Campo `canal_abertura` implementado em 2026-09-21 como `ServiceOrderChannel` (`PANEL`/`WHATSAPP`, coluna `channel`, migration V13) — fixado como `PANEL` pelo service no `create()` (mesmo padrão já usado para `status`), já que o canal `WHATSAPP` só passa a ser produzido quando o webhook do RF14 existir.
- [x] RF09 — Atribuição de OS a um técnico implementada (`PATCH /api/v1/service-orders/{id}/assign-technician/{technicianId}`), muda status para `IN_PROGRESS` e grava `AuditLog`.
- [x] RF10 — Atualização de status da OS. Implementado em 2026-09-21: `PATCH /api/v1/service-orders/{id}/status` (corpo `{status, note}`, `ADMIN`/`TECHNICIAN`) para `IN_PROGRESS` (exige técnico atribuído) e `CANCELLED` (libera o `Asset` de volta para `IN_USE`, seta `closedAt`); `RESOLVED` continua exclusivo do `/resolve` (exige laudo) e OS `RESOLVED`/`CANCELLED` não aceitam nova mudança. Todas as mudanças de status (criação, `assignTechnician`, `resolveOrder` e `changeStatus`) gravam um registro estruturado em `tb_service_order_status_logs` (V18: `previous_status`, `new_status`, `changed_by`, `note`), consultável em `GET /api/v1/service-orders/{id}/status-history` (paginado). OS criadas antes da V18 não têm histórico retroativo.
- [x] RF11 — Encerramento de OS com laudo técnico implementado (`PATCH /api/v1/service-orders/{id}/resolve`, corpo = `solutionDetails`), seta `closedAt`, volta o `Asset` vinculado para `IN_USE` e impede resolver uma OS já `RESOLVED`. Desde 2026-09-21, aceita também `?cost=` opcional (query param, retrocompatível — clientes que não enviam continuam funcionando), para registrar o custo do reparo no momento em que ele é conhecido (RF18, campo `cost` novo em `ServiceOrder`, migration V17).
- [x] RF12 — Consulta de OS: listagem paginada geral, por status (`GET /api/v1/service-orders/status/{status}`), por solicitante (`GET /api/v1/service-orders/requester/{requesterId}`), por técnico (`GET /api/v1/service-orders/technician/{technicianId}`) e por bem (`GET /api/v1/service-orders/asset/{assetId}`, RF13) implementados em 2026-09-21. Filtro por período implementado em 2026-09-21: `GET /api/v1/service-orders/period?from=yyyy-MM-dd&to=yyyy-MM-dd&field=OPENED|CLOSED` (datas inclusivas no fuso `America/Fortaleza`, ao menos uma das duas obrigatória, `field` padrão `OPENED` = data de abertura; `CLOSED` filtra pela data de conclusão/cancelamento). Parâmetros com tipo inválido (data mal formatada, enum inexistente) passaram a responder 400 (`Parâmetro Inválido`) no `GlobalExceptionHandler`, em vez do 403 vazio anterior.
- [x] RF13 — Histórico de manutenção por bem (todas as OS vinculadas a ele). Implementado em 2026-09-21: `findByAssetIdAndActiveTrue` em `ServiceOrderRepository`, método `findByAssetId` em `ServiceOrderService`/`ServiceOrderServiceImpl`, endpoint `GET /api/v1/service-orders/asset/{assetId}` (paginado) em `ServiceOrderController`. `AssetMovement` (V12) continua cobrindo apenas histórico de transferência entre setores, agora complementado por este endpoint para o histórico de manutenção/OS.

### 7.4 Integração com WhatsApp `[integração para envio de solicitações e notificações]`

> Ver seção 0.2: a integração alvo mudou de Evolution API para **WhatsApp Cloud API (Meta)**, em teste. Os RFs abaixo já descrevem o desenho para a Cloud API.

- [ ] RF14 — Recebimento de mensagens via webhook do WhatsApp Cloud API (Meta): endpoint `GET` para o desafio de verificação do webhook (`hub.verify_token` contra `WHATSAPP_WEBHOOK_VERIFY_TOKEN`) e endpoint `POST` para os eventos de mensagem, com validação de assinatura via header `X-Hub-Signature-256` (`WHATSAPP_APP_SECRET`). **Não implementado.** Nenhuma classe de webhook/controller no pacote `domain` ou em qualquer outro pacote do backend.
- [ ] RF15 — Fluxo de chatbot para abertura de chamado. **Não implementado.** Não há máquina de estados nem tabela `chatbot_sessao`.
- [ ] RF16 — Consulta de status de chamado pelo próprio WhatsApp. **Não implementado.**
- [ ] RF17 — Notificação automática ao solicitante e ao técnico via WhatsApp. **Não implementado** — nenhum evento do ciclo de vida da OS (`create`, `assignTechnician`, `resolveOrder`) dispara qualquer notificação hoje, apenas o registro em `AuditLog`.

### 7.5 Relatórios e painel `[informações para tomada de decisão]`
- [x] RF18 — Dashboard implementado em `DashboardController`/`DashboardServiceImpl` (`GET /api/v1/dashboard?companyId=`, restrito a `ADMIN`), com todos os indicadores calculados a partir de dados reais desde 2026-09-21: `totalAssets`, `assetsByCompany` e contagem de ativos por `status` (via `AssetRepository`); `activeTickets` = contagem de `ServiceOrder` ativas com status diferente de `RESOLVED`/`CANCELLED` (`ServiceOrderRepository.countActiveByCompanyId`); `maintenanceCost` = soma do novo campo `ServiceOrder.cost` (só preenchido ao resolver a OS, via RF11) das ordens resolvidas (`sumMaintenanceCostByCompanyId`); `depreciatingAssets` = contagem de ativos com `acquisitionDate` e `acquisitionValue` preenchidos (`countDepreciatingByCompanyId`) — aproximação honesta de "bens com controle de depreciação ativo", já que o sistema ainda não modela vida útil/valor residual para calcular depreciação de fato. Testado ponta a ponta com a aplicação rodando. Indicador de "tempo médio de atendimento" e "bens com mais manutenções" continuam não implementados (RF20).
- [~] RF19 — Relatório de bens por categoria/situação: o dashboard cobre "por situação" (status) e "por empresa", mas ainda não "por categoria" — `Asset.category` já existe desde o RF03 (2026-09-21), mas não há agregação por categoria no `DashboardServiceImpl` nem endpoint de relatório dedicado, só o agregado do dashboard.
- [ ] RF20 — Relatório de OS por período/técnico/status. Não implementado (nem endpoint, nem exportação CSV/PDF).

## 8. Requisitos não funcionais

- [x] Senhas com hash (bcrypt) — `BCryptPasswordEncoder` em `SecurityConfig`. Autenticação via JWT — `TokenService`/`SecurityFilter`.
- [~] Validação de entrada — Bean Validation (`@Valid` + `jakarta.validation`) aplicada nos `RequestDTO` dos módulos com DTO (assets, service orders, users, etc.). Não auditado item a item se toda regra de negócio relevante tem constraint correspondente.
- [~] Logs de auditoria — implementado para ações administrativas via `AuditLogService`/`AuditLog` (create/update/delete cobertos nos services que seguem a convenção do `CLAUDE.md`). **Auditoria de tentativas de notificação (sucesso/falha por canal) não existe**, pois não há nenhuma notificação implementada ainda.
- [~] LGPD — inativação de usuário suportada (`active = false` em soft deletes, convenção geral do projeto). Não há avaliação formal de minimização de dados pessoais coletados nem política de retenção documentada.
- [ ] Interface responsiva — fora do escopo deste repositório (backend apenas; não há frontend neste projeto).
- [ ] Webhook do WhatsApp responder rápido e processar assíncrono — não aplicável ainda, pois o webhook não existe.

## 9. Diferencial (plus): fallback de notificação via SendGrid quando o canal WhatsApp falhar

Mesmo usando a API oficial (Cloud API), o canal WhatsApp pode falhar por motivos pontuais: token de acesso expirado, número temporariamente restrito por violação de política, limite de mensagens (`messaging limit tier`) excedido, indisponibilidade passageira da Graph API, ou atraso na aprovação de um template novo. Como as notificações são parte central da proposta do TCC, o sistema não deve depender de um único canal. Este é um requisito de robustez a ser implementado após o fluxo principal de WhatsApp estar funcionando — o raciocínio do fallback continua válido mesmo com a troca da Evolution API pela Cloud API (ver seção 0.2); muda apenas a natureza do risco que ele mitiga (antes: sessão QR instável; agora: falhas pontuais de uma API oficial, ainda assim reais).

> **Status: nada desta seção foi iniciado.** Não há `NotificationSender`, `NotificationService`, `WhatsAppNotificationSender`, `EmailNotificationSender`, dependência do SendGrid no `pom.xml`, tabela `notificacao`, nem endpoint de health-check da integração WhatsApp. As variáveis `EVOLUTION_API_URL`/`EVOLUTION_API_KEY`/`EVOLUTION_INSTANCE_NAME` ainda existem em `application.yml` como placeholders da integração antiga (confirmado no `CLAUDE.md` do projeto: "declarada mas ainda não conectada a nenhum bean/cliente no código") — devem ser substituídas pelas variáveis da Cloud API listadas em 9.1 quando a implementação começar. Pré-requisito lógico (integração WhatsApp funcionando, seção 7.4) também não existe ainda, então esta seção continua corretamente bloqueada até lá.

### 9.1 Desenho da solução
- [~] Abstração `NotificationSender` — interface criada em 2026-09-21 (`domain.notification`: `channel()`/`canSend()`/`send()`). `EmailNotificationSender` (SendGrid, `RestClient` sobre `POST /v3/mail/send`, sem dependência nova) implementado e validado com envio real em 2026-09-21; **falta** o `WhatsAppNotificationSender` (Cloud API, depende da Fase 2). Sem `SENDGRID_API_KEY`/`SENDGRID_FROM_EMAIL` o canal fica desabilitado.
- [x] Um `NotificationService` central (implementado em 2026-09-21, `NotificationServiceImpl`, coberto por testes unitários; ainda sem senders reais) decide o canal: tenta WhatsApp primeiro; se a chamada falhar (erro HTTP da Graph API, timeout, token expirado, número restrito ou limite de mensagens excedido), aciona o fallback por e-mail via SendGrid automaticamente.
- [ ] Cada usuário precisa ter e-mail cadastrado (RF02) para que o fallback funcione — se não houver e-mail, registrar falha e notificar apenas por WhatsApp quando disponível. (`User.email` já existe e é obrigatório/único, então este pré-requisito de dado já está atendido.)
- [x] Disparo automático nos eventos da OS (implementado em 2026-09-21): `ServiceOrderServiceImpl` publica `ServiceOrderEvent` (`CREATED`, `TECHNICIAN_ASSIGNED`, `STATUS_CHANGED`, `RESOLVED`) e `ServiceOrderNotificationListener` (`@Async` + `@TransactionalEventListener(AFTER_COMMIT)`) notifica solicitante e técnico (sem duplicar, ignorando inativos). Falha de notificação nunca afeta a operação da OS; rollback descarta o evento. Hoje, sem WhatsApp, o único canal real é o e-mail (SendGrid).
- [x] Registrar cada tentativa de envio (canal, status, timestamp) na tabela `notificacao`, para fins de auditoria e para a discussão de resultados do TCC (mostrar taxa de sucesso por canal).
- [ ] Endpoint/health-check simples que verifica periodicamente a validade do access token e o `quality_rating`/`messaging_limit_tier` do número junto à Graph API, expondo isso no painel administrativo (ex.: badge "WhatsApp: operacional/degradado").
- [x] Templates de e-mail equivalentes às mensagens de WhatsApp (abertura de OS, atribuição, mudança de status, conclusão), incluindo o código da OS. Implementado em 2026-09-21: o `EmailNotificationSender` envia `text/plain` + `text/html` via `ServiceOrderEmailTemplate` (assunto "OS #id - título" como título; cor do cabeçalho, rótulo e barra de progresso Aberta → Em andamento → Concluída acompanham o status da OS — azul/laranja/verde, e vermelho com etapas Aberta → Cancelada; texto dinâmico escapado). O `NotificationSender.send` recebe o `ServiceOrderStatus` para isso. O texto de cada evento vem do `ServiceOrderNotificationListener`, compartilhado com o WhatsApp.
- [ ] Variáveis de ambiente sugeridas (substituem as três antigas da Evolution API): `WHATSAPP_CLOUD_API_BASE_URL` (ex.: `https://graph.facebook.com/v22.0`), `WHATSAPP_ACCESS_TOKEN`, `WHATSAPP_PHONE_NUMBER_ID`, `WHATSAPP_BUSINESS_ACCOUNT_ID`, `WHATSAPP_WEBHOOK_VERIFY_TOKEN`, `WHATSAPP_APP_SECRET`, além de `SENDGRID_API_KEY`, `SENDGRID_FROM_EMAIL`, `NOTIFICATION_FALLBACK_ENABLED`. Nenhuma delas está em `application.yml` ainda.
- [x] Retentativas com backoff simples (`notification.max-attempts`/`retry-backoff-ms`, backoff linear) antes de considerar o canal principal indisponível (evitar falso positivo por lentidão pontual).

### 9.2 Por que isso é relevante para o TCC
Reforça, na defesa, que o sistema foi pensado para continuidade operacional mesmo usando um canal gerenciado por terceiro (Meta): falhas pontuais de rate limit, token expirado ou aprovação de template são reais mesmo em API oficial. Ter um fallback documentado responde proativamente a esse ponto — e evita a pergunta mais incômoda que a banca poderia fazer com Evolution API: "o que acontece se o número for banido no meio da apresentação?" (ver seção 0.2).

## 10. Roadmap por fases

- [x] **Fase 1 — Núcleo do sistema (sem WhatsApp)**: autenticação (x), usuários com restrição de acesso por papel (x, RF01, 2026-09-21), categorias de bem (x, RF03), localizações (x), CRUD de bens completo com categoria/responsável/aquisição/estado de conservação/foto e filtros por setor/status/categoria/responsável/tombamento (x, RF03+RF05+RF06), CRUD de OS via painel com canal de abertura (x, RF08), mudança de status (x, RF10 — endpoint genérico + `os_status_log`, 2026-09-21), consultas de OS por status/solicitante/técnico/bem (x, RF12/RF13 — ver seção 7.3), histórico de transferência (x, `AssetMovement`). **Fechada** — não restam itens `~` na Fase 1 (RF12 concluído com o filtro por período).
- [ ] **Fase 2 — Integração WhatsApp (Cloud API oficial / Meta, em teste — ver seção 0.2)**: número de teste Meta ou Twilio Sandbox, webhook, máquina de estados do chatbot, abertura de OS e consulta de status via WhatsApp. **Não iniciada.**
- [ ] **Fase 3 — Notificações + fallback SendGrid**: notificações automáticas nos eventos da OS, implementação do `NotificationService` com fallback de e-mail, auditoria de envios, health-check da integração WhatsApp. **Não iniciada** (depende da Fase 2 para os eventos de disparo via WhatsApp, mas o e-mail de fallback poderia ser adiantado tecnicamente).
- [~] **Fase 4 — Relatórios e dashboard**: indicadores do dashboard 100% reais desde 2026-09-21 (RF18 — sem mocks). Falta "por categoria" no dashboard (RF19) e relatórios dedicados por categoria/período/técnico (RF19/RF20) — os agregados atuais continuam limitados ao endpoint único do dashboard.
- [~] **Fase 5 — Polimento para a banca**: responsividade (n/a, sem frontend no repo), dados de demonstração (seed) **ainda não existem** (só o usuário `admin` de bootstrap, `V16`, que não é massa de demonstração), testes dos fluxos críticos **não existem** (só o teste padrão `SgcApiApplicationTests#contextLoads`), revisão de segurança básica com `@PreAuthorize`/restrição por `Role` **concluída em 2026-09-21** (RF01).

## 11. Critérios de aceite do MVP

O MVP está pronto para demonstração quando:
1. [x] Um bem patrimonial pode ser cadastrado com tombamento, categoria, localização e responsável. *(completo desde 2026-09-21 — categoria via RF03, responsável/aquisição/estado de conservação/foto via RF05; todos os campos novos são opcionais, não quebram bens já cadastrados)*
2. [ ] Uma OS pode ser aberta tanto pelo painel quanto por uma conversa no WhatsApp, gerando o mesmo tipo de registro no banco. *(só pelo painel hoje)*
3. [~] O solicitante recebe uma notificação (WhatsApp ou, em caso de falha, e-mail) quando o status da OS muda. *(por e-mail já funciona, validado com envio real em 2026-09-21; falta o canal WhatsApp, que depende da Fase 2)*
4. [x] É possível consultar o histórico de manutenção de um bem específico. *(implementado em 2026-09-21 — RF13, `GET /api/v1/service-orders/asset/{assetId}`)*
5. [x] O painel exibe pelo menos um relatório/indicador agregando os dados de OS. *(RF18, concluído em 2026-09-21 — todos os indicadores do dashboard, incluindo os de OS, calculados a partir de dados reais)*

**3 dos 5 critérios estão 100% atendidos hoje (critérios 1, 4 e 5, todos concluídos em 2026-09-21 — via RF03+RF05+RF06, RF13 e RF18 respectivamente).** A Fase 1 e a Fase 4 (indicadores do dashboard) estão fechadas, incluindo a restrição de acesso por papel (RF01, `@PreAuthorize`). Os únicos critérios que faltam (2 e 3) dependem inteiramente da integração com WhatsApp e do mecanismo de notificação — os próximos incrementos lógicos são: validar a decisão do WhatsApp Cloud API (seção 0.2) para iniciar a Fase 2, e, em paralelo ou depois, dados de demonstração/testes automatizados (Fase 5).

## 12. Referências

Baseado no TCC "Sistema de Gerenciamento e Controle Patrimonial Integrado ao WhatsApp" (FPO, 2026), seções 1, 4, 5 e 6 (Introdução, Objetivos, Referencial Teórico e Metodologia). O item 9 (fallback SendGrid) é um acréscimo de engenharia não descrito no texto original do TCC, proposto como reforço de robustez da integração de notificações.

**Nota sobre a escrita oficial do TCC (importante):** a versão atual do texto do TCC ainda descreve a integração via **Evolution API**, conforme decidido originalmente. A mudança para **WhatsApp Cloud API** registrada na seção 0.2 deste plano (2026-09-21) é uma decisão de engenharia em teste, motivada pela onda de banimentos de APIs não oficiais do WhatsApp observada em 2026 (contas usando Evolution API/Baileys/WPPConnect/Z-API com até 40–60% de suspensão no início de 2026, sem processo de recurso). **Não altere a escrita do TCC com base só nesta nota** — a atualização do texto acadêmico (seção 6.6 e trecho correspondente da seção 9) só deve ser feita depois que a integração com a Cloud API estiver de fato implementada e validada, para não descrever no TCC algo que ainda não foi confirmado na prática.
