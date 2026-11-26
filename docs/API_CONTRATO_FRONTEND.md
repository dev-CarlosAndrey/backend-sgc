# Contrato da API — SGC API (para consumo pelo frontend)

> Atualizado em 2026-11-26 (controle de acesso por perfil e por dono: `/users` e `/employees` só ADMIN; `USER` só vê e abre as próprias OS, ver seção 4). Anterior: 2026-11-02 (avaliação do atendimento: campo `rating` na OS e nota média no relatório de OS, ver 6.10 e 6.16). Anterior: 2026-10-18 (busca `q` e filtros opcionais nas listagens, ver 3.1; corpo de `PATCH /service-orders/{id}/resolve` documentado como `text/plain`, ver 6.10; antes, 2026-10-12), a partir do código-fonte real de `../src/main/java/br/com/fpo/sgc` e das migrations `V1–V19`, e conferido contra respostas reais da API rodando (formato de paginação, filtros, CSV, RBAC, CORS). Descreve o que existe e funciona hoje no backend — sem especulação sobre funcionalidades futuras. Cobre o núcleo administrativo (Fase 1), as notificações por e-mail **e por WhatsApp**, os relatórios (CSV e PDF) e o tratamento de erro endurecido em 2026-10-12 (seção 3.2). Desde 2026-10-02, OS também podem nascer de uma mensagem de WhatsApp e ser consultadas por lá (RF15/RF16, ver 6.10/6.17) — o frontend vai ver essas OS misturadas nas mesmas listagens, com `channel: "WHATSAPP"`. Os quatro requisitos funcionais do WhatsApp (RF14–RF17) estão concluídos; não há endpoint novo que o frontend precise chamar por causa disso — é tudo mensagem de WhatsApp. Ver `PLANO_IMPLEMENTACAO_MVP.md` para o roadmap completo.

## 1. Visão geral

- **Base URL (dev local)**: `http://localhost:8080` (porta configurável via env var `PORT`).
- **Prefixo de todos os recursos de negócio**: `/api/v1/...`.
- **Formato**: JSON (`Content-Type: application/json`), exceto os dois endpoints de upload de arquivo, que usam `multipart/form-data`.
- **Autenticação**: JWT Bearer, stateless (sem sessão/cookie).
- **CORS**: liberado para qualquer origem (`allowedOriginPatterns: *`), métodos `GET, POST, PUT, PATCH, DELETE, OPTIONS`, headers `Authorization, Content-Type`, com `allowCredentials: true`. Não há necessidade de configuração adicional de CORS no frontend além de enviar o header `Authorization`.
- **Documentação interativa viva**: `GET /swagger-ui.html` (sem autenticação) — útil para conferir/testar qualquer endpoint descrito aqui em tempo real, pois é gerada a partir do mesmo código-fonte.

## 2. Autenticação

### 2.1 Login

```
POST /api/v1/auth/login
Content-Type: application/json
(sem autenticação — endpoint público)
```

**Request body:**
```json
{
  "username": "admin",
  "password": "Admin@123"
}
```

⚠️ **`username` é o campo `name` do usuário, não o e-mail.** O login busca o usuário por `name` (`UserRepository.findByNameAndActiveTrue`), então o "nome de usuário" cadastrado é literalmente o valor usado para logar — trate esse campo como um "login"/"username" na UI, não confunda com o campo `email` (que existe separadamente e serve apenas como e-mail de contato/identificação, não é usado no login hoje).

**Response 200:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "userId": 5,
  "name": "admin",
  "email": "admin@sgc.local",
  "role": "ADMIN"
}
```

- O token expira em **8 horas** (HS256, issuer `sgc-api`).
- Falha de credenciais (usuário inexistente, senha errada ou usuário inativo) retorna **401** com `ProblemDetail` (`type` `.../errors/authentication-failed`, `detail` "Usuário ou senha incorretos."). A mensagem é a mesma em todos os casos, para não revelar se o usuário existe. Até 2026-10-19 essa falha respondia 500 por engano (capturada pelo handler genérico de exceções).

### 2.2 Usar o token

Em toda requisição autenticada, enviar:
```
Authorization: Bearer <token>
```

Requisição sem token ou com token inválido/expirado em rota protegida → **403 Forbidden** (o `SecurityFilter` apenas ignora tokens inválidos e deixa a cadeia de filtros do Spring Security barrar o acesso — não é um 401 com `WWW-Authenticate`, é 403 puro).

### 2.3 Usuário administrador de bootstrap (ambiente local/dev)

Criado pela migration `V16__seed_bootstrap_admin_user.sql`:
- **username**: `admin`
- **senha**: `Admin@123`
- **role**: `ADMIN`

Necessário porque, após a implementação de RBAC (seção 4), os endpoints de escrita de `UserController` passaram a exigir `ADMIN` — sem esse usuário semente não seria possível criar o primeiro usuário via API. **Trocar essa senha antes de qualquer ambiente exposto/demonstração real.**

## 3. Convenções gerais

### 3.1 Paginação

Endpoints de listagem aceitam os parâmetros padrão do Spring Data (`?page=0&size=20&sort=campo,asc`) e retornam um envelope `Page` **serializado como DTO** (`pageSerializationMode = VIA_DTO`): os itens ficam em `content` e os metadados de paginação dentro do objeto `page`, **não** no nível de cima:

```json
{
  "content": [ /* array de itens */ ],
  "page": {
    "size": 10,
    "number": 0,
    "totalElements": 42,
    "totalPages": 5
  }
}
```

Não existem `first`, `last`, `numberOfElements`, `sort` nem `empty` — se precisar, derive: `last = page.number + 1 >= page.totalPages`, `empty = content.length === 0`. `number` é o índice da página, começando em **0**.

**Busca e filtros nas listagens** (desde 2026-10-18): os `GET /` de listagem de usuários, setores, funcionários, bens, categorias, movimentações, ordens de serviço, marcas, materiais, produtos e log de auditoria aceitam, além de `page`/`size`/`sort`, filtros opcionais por query string, todos combináveis entre si (os de cada módulo estão na tabela do módulo, seção 6):
- `q` — busca por texto livre: "contém", sem diferenciar maiúsculas/minúsculas, nos campos indicados em cada módulo. `%` e `_` são buscados literalmente (não são curingas). Vazio ou ausente = sem filtro.
- Filtros por id (`sectorId`, `categoryId`, `destinationSectorId`) e por enum (`status`, `role`, `channel`). Valor de enum inexistente → **400** (`Parâmetro Inválido`), como nos demais parâmetros (seção 3.2).

Sem nenhum filtro, a listagem continua igual à de antes. Ex.: `GET /api/v1/assets?q=notebook&status=IN_USE&sectorId=3&page=0&size=10&sort=name,asc`. As rotas antigas de filtro único (`/assets/status/{status}`, `/service-orders/status/{status}` etc.) continuam existindo.

Tamanho padrão de página (quando `size` não é enviado): **10** em usuários, bens e ordens de serviço; **20** em todos os demais (empresas, prédios, setores, funcionários, categorias, movimentações, marcas, materiais, produtos e log de auditoria). O campo de ordenação padrão varia por endpoint (documentado por módulo abaixo).

### 3.2 Formato de erro (RFC 7807 `ProblemDetail`)

Todo erro de negócio/validação/recurso-não-encontrado retorna um corpo `ProblemDetail`:

```json
{
  "type": "https://sgc.fpo.edu.br/errors/not-found",
  "title": "Recurso Não Encontrado",
  "status": 404,
  "detail": "Ativo não encontrado com ID: 99",
  "timestamp": "2026-09-21T12:00:00Z"
}
```

| Situação | HTTP status | `type` |
|---|---|---|
| Recurso não encontrado (`EntityNotFoundException`) | 404 | `.../errors/not-found` |
| Violação de regra de negócio (`BusinessException`, ex.: duplicidade) | 400 | `.../errors/business-error` |
| Arquivo de upload maior que o permitido | 400 | `.../errors/file-too-large` |
| Parâmetro de rota/query com tipo inválido (data mal formatada, valor de enum inexistente) | 400 | `.../errors/invalid-parameter`, `title: "Parâmetro Inválido"`, `detail` cita o nome do parâmetro |
| Corpo da requisição ilegível (JSON malformado, enum inválido no corpo) | 400 | `.../errors/malformed-request`, `title: "Requisição Inválida"` |
| Violação de integridade de dados no banco (chave única/FK, ex.: duas requisições quase simultâneas criando o mesmo registro) | 400 | `.../errors/data-integrity`, `title: "Conflito de Dados"` |
| Falha de validação de campos (`@Valid`) | 422 | `.../errors/invalid-data`, com `errors: { campo: mensagem }` adicional no corpo |
| Sem permissão para a ação (`@PreAuthorize` reprovado, autenticado mas sem o papel exigido) | 403 | `.../errors/access-denied`, `title: "Acesso Negado"` |
| Não autenticado / token ausente-inválido | 403 | resposta padrão do Spring Security, sem `ProblemDetail` |
| Qualquer outra exceção não prevista (rede de segurança final) | 500 | `.../errors/internal-error`, `title: "Erro Interno"`, `detail` genérico (nunca expõe stack trace ao cliente) |

Os quatro primeiros formatos de erro (404/400/422) já existiam desde o início do projeto; os três novos (`malformed-request`, `data-integrity`, `access-denied`) e a rede de segurança genérica (`internal-error`, 500) foram adicionados em 2026-10-12 para garantir que nenhuma exceção não mapeada chegasse ao cliente como erro cru do Spring — ver `GUIA_DE_TESTES.md` e `DOCUMENTACAO_TECNICA_TCC.md` para o detalhamento.

### 3.3 Soft delete e timestamps

- Nenhum recurso é excluído fisicamente. `DELETE` marca `active = false`.
- Toda entidade tem `active` (boolean) e `createdAt` (ISO-8601 UTC) nos DTOs de resposta; algumas também expõem `updatedAt` internamente, mas isso não é enviado no DTO salvo indicado abaixo.
- Listagens (`findAll`, `findBy...`) **só retornam registros com `active = true`** — exceto `GET /employees`, que sem o filtro `active` lista ativos e inativos (ver 6.6). Um recurso inativado deixa de aparecer nas listas, mas `GET /{id}` direto ainda pode retorná-lo (não há filtro de `active` na busca por ID).

### 3.4 Upload de arquivos

Dois endpoints aceitam `multipart/form-data`, campo do arquivo sempre chamado `file`:
- `POST /api/v1/companies/{id}/logo` — logo da empresa (`image/png`, `image/jpeg`, `image/svg+xml`, até 2MB).
- `POST /api/v1/assets/{id}/photo` — foto do bem patrimonial (`image/png`, `image/jpeg`, `image/webp`, até 5MB).

O limite geral do Tomcat/Spring para multipart é 5MB por arquivo e por requisição (`application.yml`). Arquivos são salvos em `uploads/<subdir>/<uuid>.<ext>` e servidos publicamente (sem autenticação) em `GET /uploads/**`. O campo `*Url`/`photoUrl`/`logoUrl` retornado já vem no formato `/uploads/<subdir>/<arquivo>` — o frontend só precisa concatenar com a base URL da API para exibir a imagem.

## 4. Perfis de acesso (RBAC)

Enum `Role`: `ADMIN`, `TECHNICIAN`, `USER` (embutido no JWT como claim `role`, e como authority Spring Security `ROLE_<role>`).

Política implementada (`@PreAuthorize` a nível de método, habilitado via `@EnableMethodSecurity`):

| Módulo | Leitura (GET) | Escrita (POST/PUT/PATCH/DELETE) |
|---|---|---|
| Usuários e Funcionários (dados pessoais: e-mail, telefone, CPF) | **ADMIN** (desde 2026-11-26, LGPD) | **ADMIN** |
| Empresas, Prédios, Setores, Marcas, Materiais, Produtos, Categorias de Bem, Ativos (incl. upload de foto/logo), Movimentações de Ativo | Qualquer autenticado | **ADMIN** |
| Ordem de Serviço — listar, buscar por ID, histórico, por solicitante | `ADMIN`/`TECHNICIAN`: todas; `USER`: **só as próprias** (403 nas alheias, desde 2026-11-26) | — |
| Ordem de Serviço — listar por bem, status, técnico, período | **ADMIN** ou **TECHNICIAN** | — |
| Ordem de Serviço — abrir (`POST`) | — | Qualquer autenticado; `USER` só em nome próprio (`requesterId` = ele mesmo, senão 403) |
| Ordem de Serviço — atribuir técnico, excluir | Qualquer autenticado | **ADMIN** |
| Ordem de Serviço — resolver, alterar status (`PATCH /{id}/status`) | — | **ADMIN** ou **TECHNICIAN** |
| Ordem de Serviço — consultas (por status/solicitante/técnico/bem/período/id/lista) e histórico de status | Qualquer autenticado | — |
| Dashboard e Relatórios (`/reports/**`) | **ADMIN** apenas | — |
| Log de auditoria | `ADMIN` (desde 2026-11-09) | — (somente leitura) |
| Webhook do WhatsApp (`/whatsapp/webhook`) | **Público** (sem JWT — a Meta não envia token; ver seção 6.17) | **Público**, mas validado por assinatura HMAC própria |

Uma chamada fora do papel permitido retorna **403 Forbidden** sem corpo `ProblemDetail` (resposta padrão do Spring Security). O frontend deve tratar 403 genericamente como "sem permissão" nesses casos, e esconder/desabilitar ações de escrita nos módulos administrativos para usuários `TECHNICIAN`/`USER`.

## 5. Enums do sistema

| Enum | Valores |
|---|---|
| `Role` | `ADMIN`, `TECHNICIAN`, `USER` |
| `AssetStatus` | `AVAILABLE`, `IN_USE`, `IN_MAINTENANCE`, `DISCARDED` |
| `AssetConservationState` | `NEW`, `GOOD`, `REGULAR`, `BAD`, `UNSERVICEABLE` |
| `ServiceOrderStatus` | `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CANCELLED` (`CANCELLED` é produzido por `PATCH /service-orders/{id}/status`; `RESOLVED` só por `PATCH /service-orders/{id}/resolve` — ver 6.10) |
| `ServiceOrderPriority` | `LOW`, `MEDIUM`, `HIGH`, `URGENT` |
| `ServiceOrderChannel` | `PANEL` (OS aberta via `POST /service-orders`) ou `WHATSAPP` (OS aberta por mensagem recebida no webhook, RF15, desde 2026-10-02) |

## 6. Endpoints por módulo

Nas tabelas abaixo, **Auth** indica o requisito de acesso: `Autenticado` (qualquer role), `ADMIN`, `ADMIN/TECHNICIAN`, ou `Público` (sem token).

### 6.1 Autenticação — `/api/v1/auth`

| Método | Path | Auth | Descrição |
|---|---|---|---|
| POST | `/login` | Público | Ver seção 2.1 |

### 6.2 Usuários — `/api/v1/users`

| Método | Path | Auth | Descrição |
|---|---|---|---|
| POST | `/` | ADMIN | Cria usuário. Body: `UserRequestDTO` |
| GET | `/{id}` | ADMIN | Busca por ID |
| GET | `/` | ADMIN | Lista paginada (`size=10`, `sort=name`). Filtros opcionais: `q` (nome ou e-mail), `role` (`Role`) |
| PUT | `/{id}` | ADMIN | Atualiza. Body: `UserRequestDTO` |
| DELETE | `/{id}` | ADMIN | Soft delete |

**`UserRequestDTO`** (request):
```json
{
  "name": "string (3-100, obrigatório, é o login)",
  "email": "string (email válido, obrigatório, único)",
  "password": "string (mín. 6, obrigatório na criação; no update, se omitido/vazio mantém a senha atual)",
  "phone": "string (opcional)",
  "role": "ADMIN | TECHNICIAN | USER (obrigatório)"
}
```

**`UserResponseDTO`** (response):
```json
{
  "id": 1,
  "name": "string",
  "email": "string",
  "phone": "string | null",
  "role": "ADMIN | TECHNICIAN | USER",
  "active": true,
  "createdAt": "2026-09-21T12:00:00Z"
}
```

### 6.3 Empresas — `/api/v1/companies`

| Método | Path | Auth | Descrição |
|---|---|---|---|
| POST | `/` | ADMIN | Cria empresa. Body: `CompanyRequestDTO` |
| GET | `/{id}` | Autenticado | Busca por ID |
| GET | `/` | Autenticado | Lista paginada |
| PUT | `/{id}` | ADMIN | Atualiza |
| DELETE | `/{id}` | ADMIN | Soft delete |
| POST | `/{id}/logo` (multipart, campo `file`) | ADMIN | Upload do logo |

**`CompanyRequestDTO`:**
```json
{
  "companyName": "string (obrigatório)",
  "cnpj": "string (obrigatório, único)",
  "email": "string (email válido, obrigatório)",
  "phone": "string (obrigatório)",
  "logoUrl": "string (opcional — normalmente não enviado manualmente, preenchido via upload)",
  "active": true
}
```

**`CompanyResponseDTO`:**
```json
{
  "id": 1,
  "companyName": "string",
  "cnpj": "string",
  "email": "string",
  "phone": "string",
  "logoUrl": "string | null",
  "active": true,
  "totalBuildings": 3,
  "totalSectors": 9
}
```
`totalSectors` conta os setores ativos de todos os prédios ativos da empresa (`SectorRepository.countByBuilding_CompanyIdAndActiveTrue`). Corrigido em 2026-11-09 — antes vinha sempre `0` (bug conhecido, ver histórico desta seção).

### 6.4 Prédios — `/api/v1/buildings`

| Método | Path | Auth | Descrição |
|---|---|---|---|
| POST | `/` | ADMIN | Cria prédio. Body: `BuildingRequestDTO` |
| GET | `/` | Autenticado | Lista paginada |
| GET | `/{id}` | Autenticado | Busca por ID |
| GET | `/company/{companyId}` | Autenticado | Lista paginada por empresa |
| PUT | `/{id}` | ADMIN | Atualiza |
| DELETE | `/{id}` | ADMIN | Soft delete |

**`BuildingRequestDTO`:**
```json
{
  "name": "string (obrigatório)",
  "address": "string (obrigatório)",
  "companyId": 1
}
```

**`BuildingResponseDTO`:**
```json
{
  "id": 1,
  "name": "string",
  "code": "string (gerado automaticamente pelo backend, ex.: BUILD-001 — não enviar no request)",
  "address": "string",
  "companyId": 1,
  "companyName": "string",
  "linkedSectorsCount": 5
}
```

### 6.5 Setores — `/api/v1/sectors`

| Método | Path | Auth | Descrição |
|---|---|---|---|
| POST | `/` | ADMIN | Cria setor. Body: `SectorRequestDTO` |
| GET | `/` | Autenticado | Lista paginada. Filtro opcional: `q` (nome do setor, do prédio ou do responsável) |
| GET | `/{id}` | Autenticado | Busca por ID |
| GET | `/building/{buildingId}` | Autenticado | Lista paginada por prédio |
| PUT | `/{id}` | ADMIN | Atualiza |
| DELETE | `/{id}` | ADMIN | Soft delete |

**`SectorRequestDTO`:**
```json
{
  "name": "string (2-100, obrigatório)",
  "buildingId": 1,
  "active": true,
  "managerId": 5
}
```
`managerId` referencia um `Employee` (funcionário), não um `User`.

**`SectorResponseDTO`:**
```json
{
  "id": 1,
  "name": "string",
  "building": { /* BuildingResponseDTO */ },
  "managerId": 5,
  "managerName": "string | null",
  "managerAvatar": null,
  "totalAssets": 12,
  "activeTickets": 2,
  "active": true,
  "createdAt": "2026-09-21T12:00:00Z"
}
```
`managerAvatar` está sempre `null` hoje (campo reservado, não implementado).

### 6.6 Funcionários — `/api/v1/employees`

| Método | Path | Auth | Descrição |
|---|---|---|---|
| POST | `/` | ADMIN | Cria funcionário. Body: `EmployeeRequestDTO` |
| GET | `/` | ADMIN | Lista paginada (`size=20`). Filtros opcionais: `q` (nome, CPF ou matrícula), `sectorId`, `active` (`true`/`false`). ⚠️ Sem `active`, lista **ativos e inativos** (exceção à regra da seção 3.3) |
| GET | `/{id}` | ADMIN | Busca por ID |
| PUT | `/{id}` | ADMIN | Atualiza |
| DELETE | `/{id}` | ADMIN | Soft delete |

**`EmployeeRequestDTO`:**
```json
{
  "name": "string (obrigatório)",
  "cpf": "string (obrigatório, único)",
  "registrationNumber": "string (matrícula, obrigatório, único)",
  "email": "string (opcional)",
  "phone": "string (opcional)",
  "position": "string (cargo, opcional)",
  "admissionDate": "2026-01-15 (opcional, LocalDate)",
  "companyId": 1,
  "sectorId": 3
}
```

**`EmployeeResponseDTO`:**
```json
{
  "id": 1,
  "name": "string",
  "cpf": "string",
  "registrationNumber": "string",
  "email": "string | null",
  "phone": "string | null",
  "position": "string | null",
  "admissionDate": "2026-01-15",
  "active": true,
  "companyId": 1,
  "companyName": "string",
  "sectorId": 3,
  "sectorName": "string | null",
  "createdAt": "2026-09-21T12:00:00Z"
}
```

### 6.7 Bens patrimoniais (Assets) — `/api/v1/assets`

| Método | Path | Auth | Descrição |
|---|---|---|---|
| POST | `/` | ADMIN | Cria bem. Body: `AssetRequestDTO` |
| GET | `/{id}` | Autenticado | Busca por ID |
| GET | `/tag/{tagNumber}` | Autenticado | Busca por tombamento |
| GET | `/` | Autenticado | Lista paginada (`size=10`, `sort=name`). Filtros opcionais e combináveis: `q` (tombamento, nome, modelo ou nº de série), `status` (`AssetStatus`), `sectorId`, `categoryId` |
| GET | `/sector/{sectorId}` | Autenticado | Lista paginada por setor |
| GET | `/status/{status}` | Autenticado | Lista paginada por status (`AssetStatus`) |
| GET | `/category/{categoryId}` | Autenticado | Lista paginada por categoria |
| GET | `/responsible/{responsibleId}` | Autenticado | Lista paginada por responsável (funcionário) |
| PUT | `/{id}` | ADMIN | Atualiza |
| DELETE | `/{id}` | ADMIN | Soft delete |
| POST | `/{id}/photo` (multipart, campo `file`) | ADMIN | Upload de foto |

**`AssetRequestDTO`:**
```json
{
  "tagNumber": "string (obrigatório, único, tombamento)",
  "name": "string (2-120, obrigatório)",
  "description": "string (opcional)",
  "model": "string (opcional)",
  "serialNumber": "string (opcional)",
  "status": "AVAILABLE | IN_USE | IN_MAINTENANCE | DISCARDED (obrigatório)",
  "sectorId": 1,
  "categoryId": 2,
  "responsibleId": 5,
  "acquisitionDate": "2025-01-10 (opcional, não pode ser futura)",
  "acquisitionValue": 1500.00,
  "conservationState": "NEW | GOOD | REGULAR | BAD | UNSERVICEABLE (opcional)"
}
```
`categoryId`, `responsibleId`, `acquisitionDate`, `acquisitionValue`, `conservationState` são **todos opcionais** — um ativo cadastrado sem eles continua válido.

**`AssetResponseDTO`:**
```json
{
  "id": 1,
  "tagNumber": "string",
  "name": "string",
  "description": "string | null",
  "model": "string | null",
  "serialNumber": "string | null",
  "status": "AVAILABLE | IN_USE | IN_MAINTENANCE | DISCARDED",
  "sector": { /* SectorResponseDTO */ },
  "category": { /* AssetCategoryResponseDTO | null */ },
  "responsible": { /* EmployeeResponseDTO | null */ },
  "acquisitionDate": "2025-01-10 | null",
  "acquisitionValue": 1500.00,
  "conservationState": "NEW | GOOD | REGULAR | BAD | UNSERVICEABLE | null",
  "photoUrl": "/uploads/assets/uuid.jpg | null",
  "active": true,
  "createdAt": "2026-09-21T12:00:00Z"
}
```

### 6.8 Categorias de bem — `/api/v1/asset-categories`

| Método | Path | Auth | Descrição |
|---|---|---|---|
| POST | `/` | ADMIN | Cria categoria. Body: `AssetCategoryRequestDTO` |
| GET | `/` | Autenticado | Lista paginada (`size=20`, `sort=name`). Filtro opcional: `q` (nome ou descrição) |
| GET | `/{id}` | Autenticado | Busca por ID |
| PUT | `/{id}` | ADMIN | Atualiza |
| DELETE | `/{id}` | ADMIN | Soft delete |

**`AssetCategoryRequestDTO`:**
```json
{
  "name": "string (2-100, obrigatório, único)",
  "description": "string (até 255, opcional)",
  "active": true
}
```

**`AssetCategoryResponseDTO`:**
```json
{
  "id": 1,
  "name": "string",
  "description": "string | null",
  "active": true,
  "createdAt": "2026-09-21T12:00:00Z"
}
```

### 6.9 Movimentações de bem — `/api/v1/asset-movements`

Histórico de transferência de um bem entre setores (não é o mesmo que histórico de manutenção — ver 6.10, seção Ordens de Serviço).

| Método | Path | Auth | Descrição |
|---|---|---|---|
| POST | `/` | ADMIN | Registra movimentação. Body: `AssetMovementRequestDTO` |
| GET | `/` | Autenticado | Lista paginada (`size=20`, `sort=movementDate DESC`). Filtros opcionais: `q` (tombamento/nome do bem, setor de origem ou de destino), `destinationSectorId` |
| GET | `/asset/{assetId}` | Autenticado | Lista paginada por bem |

**`AssetMovementRequestDTO`:**
```json
{
  "assetId": 1,
  "destinationSectorId": 3,
  "reason": "string (opcional)"
}
```
O setor de origem é inferido automaticamente a partir do setor atual do bem no momento do registro (não é enviado pelo cliente).

**`AssetMovementResponseDTO`:**
```json
{
  "id": 1,
  "assetId": 1,
  "assetTagNumber": "string",
  "assetName": "string",
  "originSectorId": 2,
  "originSectorName": "string",
  "destinationSectorId": 3,
  "destinationSectorName": "string",
  "reason": "string | null",
  "movementDate": "2026-09-21T12:00:00Z"
}
```

### 6.10 Ordens de Serviço — `/api/v1/service-orders`

| Método | Path | Auth | Descrição |
|---|---|---|---|
| POST | `/` | Autenticado | Abre chamado. Body: `ServiceOrderRequestDTO` |
| GET | `/{id}` | Autenticado (USER: só as próprias) | Busca por ID |
| GET | `/` | Autenticado (USER: só as próprias) | Lista paginada (`size=10`, `sort=createdAt` — envie `sort=createdAt,desc` para as mais recentes primeiro). Filtros opcionais e combináveis: `q` (título, nome do solicitante ou do setor), `status` (`ServiceOrderStatus`), `channel` (`ServiceOrderChannel`) |
| GET | `/asset/{assetId}` | ADMIN/TECHNICIAN | **Histórico de manutenção do bem** — todas as OS vinculadas a ele |
| GET | `/status/{status}` | ADMIN/TECHNICIAN | Lista paginada por status |
| GET | `/requester/{requesterId}` | Autenticado (USER: só as próprias) | Lista paginada por solicitante |
| GET | `/technician/{technicianId}` | ADMIN/TECHNICIAN | Lista paginada por técnico |
| GET | `/period?from=2026-09-01&to=2026-09-30&field=OPENED` | ADMIN/TECHNICIAN | Lista paginada por período (ver abaixo) |
| GET | `/{id}/status-history` | Autenticado (USER: só as próprias) | Histórico de mudanças de status da OS (paginado) |
| PATCH | `/{id}/assign-technician/{technicianId}` | ADMIN | Atribui técnico, muda status para `IN_PROGRESS` |
| PATCH | `/{id}/resolve?cost=150.75` (query param `cost` opcional) | ADMIN ou TECHNICIAN | Resolve a OS, muda status para `RESOLVED` |
| PATCH | `/{id}/status` | ADMIN ou TECHNICIAN | Altera o status para `IN_PROGRESS` ou `CANCELLED` (ver abaixo) |
| DELETE | `/{id}` | ADMIN | Soft delete |

**`ServiceOrderRequestDTO`** (usado em `POST /`):
```json
{
  "title": "string (5-150, obrigatório)",
  "description": "string (obrigatório)",
  "priority": "LOW | MEDIUM | HIGH | URGENT (obrigatório)",
  "requesterId": 1,
  "assetId": 5,
  "sectorId": 1
}
```
`assetId` é opcional (chamado pode não estar vinculado a um bem específico). Ao vincular um `assetId`, o backend automaticamente muda o `status` do bem para `IN_MAINTENANCE`. Campos que o cliente **não** envia e são definidos automaticamente pelo backend: `status` (sempre inicia `OPEN`) e `channel` (sempre `PANEL` hoje).

**`PATCH /{id}/resolve`** — corpo e query param, não um DTO único:
```
PATCH /api/v1/service-orders/5/resolve?cost=150.75
Content-Type: text/plain

Laudo técnico / solução aplicada em texto livre
(pode ter várias linhas)
```
O corpo é o **laudo em texto puro** (`Content-Type: text/plain`), sem aspas e sem objeto `{ solutionDetails: ... }`. O controller recebe `@RequestBody String` e grava o corpo exatamente como chega. Por isso **não envie como string JSON** (`"..."` com `application/json`): as aspas e os `\n` escapados seriam salvos literalmente no laudo. No axios, passe a string com `headers: { 'Content-Type': 'text/plain' }`. `cost` é um **query param opcional** (`BigDecimal`) — se omitido, a OS é resolvida sem custo registrado. Retorna 400 (`BusinessException`) se a OS já estiver `RESOLVED` **ou `CANCELLED`** (uma OS cancelada não pode ser resolvida).

**`ServiceOrderResponseDTO`:**
```json
{
  "id": 1,
  "title": "string",
  "description": "string",
  "status": "OPEN | IN_PROGRESS | RESOLVED | CANCELLED",
  "priority": "LOW | MEDIUM | HIGH | URGENT",
  "channel": "PANEL | WHATSAPP",
  "requester": { /* UserResponseDTO */ },
  "technician": { /* UserResponseDTO | null */ },
  "asset": { /* AssetResponseDTO | null */ },
  "sector": { /* SectorResponseDTO */ },
  "solutionDetails": "string | null",
  "closedAt": "2026-09-21T12:00:00Z | null",
  "cost": 150.75,
  "active": true,
  "createdAt": "2026-09-21T12:00:00Z",
  "rating": 5
}
```

`rating` (desde 2026-11-02): nota de 1 a 5 dada pelo solicitante pelo WhatsApp depois da conclusão (`Nota #<id> <nota>`, ver 6.17); `null` enquanto não avaliada. Só existe para OS `RESOLVED`, é dada uma única vez e não tem endpoint no painel.

**`GET /period`** — filtro por período (RF12):
- `from` e `to`: datas `yyyy-MM-dd`, **inclusivas** nas duas pontas (fuso `America/Fortaleza`). Ao menos uma é obrigatória; a outra pode ser omitida.
- `field`: `OPENED` (padrão, data de abertura) ou `CLOSED` (data de conclusão/cancelamento — OS ainda abertas não aparecem).
- `from` posterior a `to`, ou nenhuma das duas datas → **400**. Formato de data inválido ou `field` inexistente → **400** (`Parâmetro Inválido`).
- Vale também para qualquer parâmetro de rota/query com tipo inválido na API (ex.: `/status/XYZ` responde 400).

**`PATCH /{id}/status`** — alteração de status (RF10):
```json
{ "status": "IN_PROGRESS | CANCELLED", "note": "string opcional" }
```
Regras (violação → **400**): `IN_PROGRESS` exige técnico já atribuído; `CANCELLED` libera o bem vinculado de volta para `IN_USE`; **`RESOLVED` não é aceito aqui** (use `/resolve`, que exige laudo); não é possível reabrir (`OPEN`), repetir o status atual nem alterar uma OS já `RESOLVED`/`CANCELLED`. `status` ausente → 422.

**`GET /{id}/status-history`** — cada item:
```json
{
  "id": 1,
  "serviceOrderId": 5,
  "previousStatus": "OPEN | IN_PROGRESS | ... | null",
  "newStatus": "OPEN | IN_PROGRESS | RESOLVED | CANCELLED",
  "changedBy": "nome do usuário autenticado (ou SYSTEM)",
  "note": "string | null",
  "createdAt": "2026-09-21T12:00:00Z"
}
```
`previousStatus` é `null` no registro de abertura. OS criadas antes de 2026-09-21 não têm histórico retroativo.

**Notificações:** criar, atribuir técnico, alterar status e resolver disparam notificação ao solicitante e ao técnico atribuído (na resolução, também a quem resolveu a OS, mesmo que não seja o técnico atribuído). O backend tenta **WhatsApp primeiro** (se `WHATSAPP_ACCESS_TOKEN`/`WHATSAPP_PHONE_NUMBER_ID` estiverem configurados e o usuário tiver telefone) e cai automaticamente para **e-mail** via SendGrid em caso de falha (se `SENDGRID_API_KEY` estiver configurada e o usuário tiver e-mail). Mensagem de WhatsApp é texto livre, então só é entregue dentro da janela de atendimento de 24h da Cloud API — quem abriu a OS pelo próprio WhatsApp (ver 6.17) está dentro dessa janela; quem nunca escreveu para o número (ex.: um técnico atribuído) tende a cair no fallback de e-mail. É assíncrono e não interfere na resposta da API; não há endpoint para consultar notificações.

**OS abertas via WhatsApp (RF15):** o solicitante (`requester`) pode ser um `User` criado automaticamente pelo backend (role `USER`, e-mail sintético no formato `whatsapp+<telefone>@sgc.local`) — qualquer pessoa pode abrir chamado pelo WhatsApp sem cadastro prévio no painel. Não há nada de especial para o frontend tratar: essas OS aparecem nas mesmas listagens, só com `channel: "WHATSAPP"` e, possivelmente, um `requester.email` reconhecível pelo prefixo `whatsapp+`.

### 6.11 Marcas — `/api/v1/brands`

| Método | Path | Auth | Descrição |
|---|---|---|---|
| POST | `/` | ADMIN | Cria marca |
| GET | `/{id}` | Autenticado | Busca por ID |
| GET | `/` | Autenticado | Lista paginada. Filtro opcional: `q` (nome) |
| PUT | `/{id}` | ADMIN | Atualiza |
| DELETE | `/{id}` | ADMIN | Soft delete |

**`BrandRequestDTO`:** `{ "name": "string (obrigatório)" }`
**`BrandResponseDTO`:** `{ "id": 1, "name": "string", "active": true }`

### 6.12 Materiais — `/api/v1/materials`

| Método | Path | Auth | Descrição |
|---|---|---|---|
| POST | `/` | ADMIN | Cria material |
| GET | `/` | Autenticado | Lista paginada (`size=20`). Filtro opcional: `q` (nome ou categoria) |
| GET | `/{id}` | Autenticado | Busca por ID |
| PUT | `/{id}` | ADMIN | Atualiza |
| DELETE | `/{id}` | ADMIN | Soft delete |

**`MaterialRequestDTO`:**
```json
{ "name": "string (2-100, obrigatório)", "category": "string (2-100, obrigatório)", "active": true }
```
**`MaterialResponseDTO`:**
```json
{ "id": 1, "name": "string", "category": "string", "active": true, "createdAt": "2026-09-21T12:00:00Z" }
```

### 6.13 Produtos — `/api/v1/products`

Catálogo auxiliar que combina `Brand` + `Material`. **Não vinculado a `Asset`** — não confundir com a categoria de bem (`AssetCategory`, seção 6.8), que é o mecanismo real de categorização de ativos.

| Método | Path | Auth | Descrição |
|---|---|---|---|
| POST | `/` | ADMIN | Cria produto |
| GET | `/` | Autenticado | Lista paginada (`size=20`). Filtro opcional: `q` (nome do produto, da marca, do material ou categoria do material) |
| GET | `/{id}` | Autenticado | Busca por ID |
| PUT | `/{id}` | ADMIN | Atualiza |
| DELETE | `/{id}` | ADMIN | Soft delete |

**`ProductRequestDTO`:**
```json
{
  "name": "string (2-150, obrigatório)",
  "description": "string (opcional)",
  "brandId": 1,
  "materialId": 2,
  "active": true
}
```
**`ProductResponseDTO`:**
```json
{
  "id": 1, "name": "string", "description": "string | null",
  "brandId": 1, "brandName": "string",
  "materialId": 2, "materialName": "string", "category": "string",
  "active": true, "createdAt": "2026-09-21T12:00:00Z"
}
```

### 6.14 Log de auditoria — `/api/v1/audit-logs`

Somente leitura. Registrado automaticamente pelo backend em toda operação de create/update/delete relevante (não há endpoint de escrita). Restrito a `ADMIN` desde 2026-11-09 (antes, qualquer autenticado conseguia ler o log de auditoria de todo o sistema).

| Método | Path | Auth | Descrição |
|---|---|---|---|
| GET | `/` | `ADMIN` | Lista paginada (`size=20`, `sort=createdAt DESC`). Filtro opcional: `q` (entidade, ação, responsável ou detalhes) |
| GET | `/{entityName}/{entityId}` | `ADMIN` | Histórico de um recurso específico (ex.: `/Asset/5`) |

**`AuditLogResponseDTO`:**
```json
{
  "id": 1,
  "entityName": "Asset",
  "entityId": 5,
  "action": "CREATE | UPDATE | DELETE | ASSIGN_TECHNICIAN | RESOLVE | CHANGE_STATUS | ...",
  "performedBy": "string (nome do usuário autenticado que executou a ação — o mesmo valor do login —, ou SYSTEM se não havia usuário)",
  "details": "string (texto livre descrevendo a ação)",
  "createdAt": "2026-09-21T12:00:00Z"
}
```

### 6.15 Dashboard — `/api/v1/dashboard`

| Método | Path | Auth | Descrição |
|---|---|---|---|
| GET | `/?companyId=1` (query param opcional) | **ADMIN apenas** | Indicadores agregados |

Sem `companyId`, os indicadores são globais (todas as empresas); com `companyId`, **todos** os indicadores (inclusive `assetsByStatus` e `assetsByCompany`) são restritos àquela empresa — um `companyId` sem bens devolve tudo zerado. `assetsByStatus` traz sempre as 4 situações, zeradas inclusive. Todos os valores são calculados a partir de dados reais (sem mocks, desde 2026-09-21):

```json
{
  "totalAssets": 42,
  "activeTickets": 5,
  "maintenanceCost": 1250.50,
  "depreciatingAssets": 10,
  "assetsByStatus": { "AVAILABLE": 30, "IN_USE": 8, "IN_MAINTENANCE": 3, "DISCARDED": 1 },
  "assetsByCompany": { "Faculdade Princesa do Oeste - FPO": 35, "Escola Sônia Burgos": 7 }
}
```

- `activeTickets` = OS com status diferente de `RESOLVED`/`CANCELLED`.
- `maintenanceCost` = soma do campo `cost` das OS resolvidas (só existe se o `cost` foi informado ao resolver — ver 6.10).
- `depreciatingAssets` = quantidade de ativos com `acquisitionDate` **e** `acquisitionValue` preenchidos (aproximação — o sistema ainda não modela vida útil/valor residual para depreciação de fato).

### 6.16 Relatórios — `/api/v1/reports` (**ADMIN apenas**)

| Método | Path | Descrição |
|---|---|---|
| GET | `/assets?companyId=` | Bens por categoria e situação (RF19) |
| GET | `/assets/export?companyId=` | Mesmo relatório em **CSV** |
| GET | `/assets/export/pdf?companyId=` | Mesmo relatório em **PDF** |
| GET | `/service-orders` | Indicadores de OS por período/técnico/status (RF20) |
| GET | `/service-orders/export` | Lista de OS do filtro em **CSV** |
| GET | `/service-orders/export/pdf` | Mesma lista em **PDF** (paisagem) |
| GET | `/maintenance-cost` | Custo de manutenção × valor de aquisição por bem |
| GET | `/maintenance-cost/export` | Mesmo relatório em **CSV** |
| GET | `/maintenance-cost/export/pdf` | Mesmo relatório em **PDF** |
| GET | `/service-orders/monthly` | Evolução mensal: OS abertas × concluídas e custo por mês |
| GET | `/service-orders/stage-times` | Tempo médio em cada etapa do fluxo da OS e gargalo |
| GET | `/assets/patrimony` | Patrimônio com depreciação linear por categoria ou setor |
| GET | `/assets/critical` | Bens críticos (estado ruim e/ou manutenção recorrente) |
| GET | `/movements` | Resumo das movimentações de bens no período |
| GET | `/audit` | Resumo do log de auditoria (por entidade, ação e usuário) |
| GET | `/service-orders/channels` | OS por canal de abertura (painel × WhatsApp) e evolução mensal |
| GET | `/notifications` | Entrega das notificações: sucesso por canal, desfecho, taxa de fallback e erros |
| GET | `<qualquer um acima>/export` | Mesmo relatório em **CSV** (`os-por-mes.csv`, `tempo-por-etapa.csv`, `patrimonio.csv`, `bens-criticos.csv`, `movimentacoes.csv`, `auditoria.csv`, `os-por-canal.csv`, `entrega-notificacoes.csv`) |
| GET | `<qualquer um acima>/export/pdf` | Mesmo relatório em **PDF** (mesmos nomes de arquivo, extensão `.pdf`) |

Filtros de `/service-orders` e `/service-orders/export` (todos opcionais; sem nenhum, cobre todas as OS ativas): `from`, `to` (datas `yyyy-MM-dd` inclusivas), `field` (`OPENED` padrão | `CLOSED`, indica a qual data o período se aplica), `technicianId`, `status`, `companyId`. `from` > `to` → 400.

**`GET /reports/assets`**
```json
{
  "total": 4,
  "byStatus": [ { "key": "AVAILABLE", "total": 2 }, { "key": "IN_USE", "total": 1 }, { "key": "IN_MAINTENANCE", "total": 1 }, { "key": "DISCARDED", "total": 0 } ],
  "byCategory": [
    { "categoryId": 1, "categoryName": "Informática", "total": 1,
      "byStatus": { "AVAILABLE": 0, "IN_USE": 1, "IN_MAINTENANCE": 0, "DISCARDED": 0 } },
    { "categoryId": null, "categoryName": "Sem categoria", "total": 3, "byStatus": { "AVAILABLE": 2, "IN_USE": 0, "IN_MAINTENANCE": 1, "DISCARDED": 0 } }
  ]
}
```
`byStatus` e `byCategory[].byStatus` sempre trazem **todas** as situações (zeradas inclusive). Bens sem categoria vêm com `categoryId: null`. Categorias ordenadas da maior para a menor.

**`GET /reports/service-orders`**
```json
{
  "total": 6,
  "resolved": 3,
  "totalCost": 61.00,
  "avgResolutionHours": 2.5,
  "byStatus":   [ { "key": "OPEN", "total": 3 }, { "key": "IN_PROGRESS", "total": 0 }, { "key": "RESOLVED", "total": 3 }, { "key": "CANCELLED", "total": 0 } ],
  "byPriority": [ { "key": "LOW", "total": 2 }, { "key": "MEDIUM", "total": 1 }, { "key": "HIGH", "total": 3 }, { "key": "URGENT", "total": 0 } ],
  "byTechnician": [ { "technicianId": 5, "technicianName": "admin", "total": 3, "resolved": 3, "avgResolutionHours": 2.5, "avgRating": 4.5 },
                    { "technicianId": null, "technicianName": "Sem técnico", "total": 3, "resolved": 0, "avgResolutionHours": null, "avgRating": null } ],
  "topAssets": [ { "assetId": 4, "tagNumber": "REL-001", "name": "Notebook", "total": 2 } ],
  "ratedOrders": 2,
  "avgRating": 4.5
}
```
- `avgResolutionHours` = média entre abertura e conclusão das OS **`RESOLVED`** do filtro (canceladas não contam); `null` se não houver nenhuma. Arredondado a 1 casa.
- `totalCost` = soma do `cost` das OS do filtro.
- `topAssets` = até 5 bens com mais OS no filtro (mais manutenções).
- `ratedOrders` / `avgRating` (desde 2026-11-02) = OS do filtro avaliadas pelo solicitante e a nota média (1 a 5, 1 casa); `avgRating` também por técnico. `null` quando nenhuma foi avaliada.

**`GET /reports/maintenance-cost`** — filtros opcionais: `from`, `to` (`yyyy-MM-dd` inclusivas), `field` (`CLOSED` padrão | `OPENED`), `companyId`, `threshold` (% padrão `50`; negativo → 400)
```json
{
  "thresholdPercent": 50.0,
  "totalMaintenanceCost": 1380.00,
  "totalAcquisitionValue": 3000.00,
  "assetsAboveThreshold": 1,
  "assets": [
    { "assetId": 1, "tagNumber": "T-001", "name": "Notebook", "categoryName": "Informática",
      "acquisitionValue": 2000.00, "orders": 2, "maintenanceCost": 1200.00,
      "costPercentOfValue": 60.0, "exceedsThreshold": true },
    { "assetId": 3, "tagNumber": "T-003", "name": "Doado", "categoryName": "Sem categoria",
      "acquisitionValue": null, "orders": 1, "maintenanceCost": 80.00,
      "costPercentOfValue": null, "exceedsThreshold": false }
  ]
}
```
- Considera só OS **`RESOLVED`** com `cost` preenchido e com bem vinculado. Bens sem nenhuma OS assim não aparecem.
- `costPercentOfValue` = custo acumulado / `acquisitionValue` × 100 (1 casa). `null` se o bem não tem valor de aquisição (ou é zero); nesse caso `exceedsThreshold` é sempre `false`.
- `exceedsThreshold` = `costPercentOfValue >= threshold`: bem candidato a substituição/baixa.
- Ordenado por `costPercentOfValue` decrescente (nulos por último). `totalAcquisitionValue` soma só os bens listados. CSV: `custo-manutencao-por-bem.csv`.

#### Relatórios de análise

Todos `ADMIN`, todos com `/export` (CSV, mesmo formato descrito abaixo). Datas `yyyy-MM-dd` inclusivas; `from` > `to` → 400.

**`GET /reports/service-orders/monthly`** — `from`, `to`, `companyId`. Padrão: últimos 12 meses; máximo 60 (senão 400). O período é ajustado a meses inteiros.
```json
{ "totalOpened": 9, "totalResolved": 6,
  "months": [ { "month": "2026-08", "opened": 0, "resolved": 1, "cost": 40.00 },
              { "month": "2026-09", "opened": 9, "resolved": 5, "cost": 320.50 } ] }
```
`opened` conta por data de abertura; `resolved` e `cost` contam por data de **conclusão** (OS `RESOLVED`). Meses sem movimento vêm zerados; ordem cronológica.

**`GET /reports/service-orders/stage-times`** — `from`, `to` (aplicados à abertura da OS), `companyId`. Calculado sobre o histórico de status.
```json
{ "orders": 12, "bottleneck": "OPEN",
  "stages": [ { "status": "OPEN", "samples": 10, "avgHours": 6.5, "maxHours": 30.0 },
              { "status": "IN_PROGRESS", "samples": 8, "avgHours": 2.1, "maxHours": 5.0 } ],
  "byPriority": [ { "priority": "HIGH", "orders": 4, "avgFirstResponseHours": 1.5, "avgResolutionHours": 4.0 } ] }
```
- `stages` só traz as etapas com permanência (`OPEN`, `IN_PROGRESS`); só passagens já encerradas contam. `bottleneck` = maior média (`null` sem amostras).
- `avgFirstResponseHours` = tempo em `OPEN` até a primeira mudança de status; `avgResolutionHours` = da abertura à conclusão (só `RESOLVED`). `byPriority` traz todas as prioridades (zeradas/`null` inclusive).

**`GET /reports/assets/patrimony`** — `companyId`, `groupBy` (`CATEGORY` padrão | `SECTOR`), `usefulLifeYears` (padrão `10`, 1–50), `asOf` (padrão hoje).
```json
{ "asOf": "2026-09-21", "usefulLifeYears": 10, "assets": 4, "assetsWithoutValuation": 1,
  "acquisitionValue": 2000.00, "accumulatedDepreciation": 1100.00, "bookValue": 900.00,
  "groups": [ { "groupId": 1, "groupName": "Informática", "assets": 3, "assetsWithoutValuation": 0,
                "acquisitionValue": 2000.00, "accumulatedDepreciation": 1100.00, "bookValue": 900.00 } ] }
```
Depreciação **linear, sem valor residual, por meses completos**, com a mesma vida útil para todos os bens (o sistema não modela vida útil por categoria); bem totalmente depreciado vale 0. Bens baixados (`DISCARDED`) ficam de fora; bens sem `acquisitionDate` ou `acquisitionValue` contam em `assetsWithoutValuation` e não entram nos valores. Grupos por valor contábil decrescente; `groupId: null` = "Sem categoria".

**`GET /reports/assets/critical`** — `companyId`, `from`/`to` (aplicados à abertura das OS), `minOrders` (padrão `2`, ≥ 1).
```json
{ "minOrders": 2, "total": 1,
  "assets": [ { "assetId": 3, "tagNumber": "T-3", "name": "Impressora", "sectorName": "Sala 1", "categoryName": "Informática",
                "conservationState": "BAD", "orders": 3, "maintenanceCost": 120.00,
                "reasons": ["BAD_CONSERVATION", "RECURRENT_MAINTENANCE"] } ] }
```
`reasons`: `BAD_CONSERVATION` (estado `BAD`/`UNSERVICEABLE`) e/ou `RECURRENT_MAINTENANCE` (≥ `minOrders` OS não canceladas). Ordenado: mais motivos, mais OS, maior custo. Bens baixados/inativos ficam de fora.

**`GET /reports/movements`** — `from`, `to`, `assetId`, `companyId` (setor de destino). Só resumo; a lista completa (data, bem, origem, destino, motivo) está no CSV.
```json
{ "total": 3, "distinctAssets": 2,
  "topAssets": [ { "assetId": 1, "tagNumber": "T-1", "name": "Notebook", "total": 2 } ],
  "byOriginSector": [ { "key": "Sala 1", "total": 2 } ], "byDestinationSector": [ { "key": "Sala 3", "total": 2 } ] }
```

**`GET /reports/audit`** — `from`, `to`, `entityName`, `performedBy` (igualdade exata). Resumo com `total`, `byEntity`, `byAction`, `byUser` (`{key,total}`, maior primeiro; usuário nulo aparece como "Sem usuário"). O CSV traz os **10.000 eventos mais recentes** do filtro.

**`GET /reports/service-orders/channels`** — `from`, `to` (aplicados à abertura da OS), `companyId`. Desde 2026-10-19.
```json
{ "total": 52,
  "channels": [ { "channel": "PANEL", "total": 23, "sharePercent": 44.2, "resolved": 19, "cancelled": 1, "pending": 3, "avgResolutionHours": 80.7 },
                { "channel": "WHATSAPP", "total": 29, "sharePercent": 55.8, "resolved": 24, "cancelled": 2, "pending": 3, "avgResolutionHours": 37.6 } ],
  "months": [ { "month": "2026-05", "panel": 2, "whatsapp": 5, "whatsappPercent": 71.4 } ] }
```
- `channels` traz sempre os dois canais (`PANEL`, `WHATSAPP`), zerados inclusive. `pending` = `OPEN` + `IN_PROGRESS`. `avgResolutionHours` considera só OS `RESOLVED` (`null` se não houver). Percentuais com 1 casa; `null` quando não há OS no período.
- `months`: só meses com OS abertas, em ordem cronológica (fuso `America/Fortaleza`). OS anteriores à coluna `channel` contam como `PANEL`.

**`GET /reports/notifications`** — `from`, `to` (aplicados à data de cada tentativa), `companyId` (empresa do setor da OS). Desde 2026-10-19.
```json
{ "totalAttempts": 642, "totalNotifications": 237, "delivered": 237,
  "deliveryRatePercent": 100.0, "fallbackRatePercent": 67.5,
  "channels": [ { "channel": "WHATSAPP", "attempts": 470, "sent": 65, "failed": 405, "successRatePercent": 13.8 },
                { "channel": "EMAIL", "attempts": 172, "sent": 172, "failed": 0, "successRatePercent": 100.0 } ],
  "byOutcome": [ { "key": "WHATSAPP_FIRST_ATTEMPT", "total": 65 }, { "key": "WHATSAPP_AFTER_RETRY", "total": 0 },
                 { "key": "EMAIL_FALLBACK", "total": 135 }, { "key": "EMAIL_ONLY", "total": 37 }, { "key": "NOT_DELIVERED", "total": 0 } ],
  "topErrors": [ { "key": "(#131047) Re-engagement message: More than 24 hours have passed since the recipient last replied to the sender number.", "total": 405 } ] }
```
- **Tentativa** = uma linha de `tb_notifications` (um envio em um canal). **Notificação** = um destinatário em um evento da OS, com todas as suas tentativas (WhatsApp com retentativas e, se falhar, e-mail).
- `channels`: tentativas por canal; `successRatePercent` = `sent` / `attempts`.
- `byOutcome` (sempre os 5): entregue por WhatsApp na 1ª tentativa, por WhatsApp após retentativa, por e-mail depois de o WhatsApp falhar (`EMAIL_FALLBACK`), só por e-mail porque o WhatsApp não se aplicava (`EMAIL_ONLY`: destinatário sem telefone ou canal não configurado) ou `NOT_DELIVERED`.
- `deliveryRatePercent` = notificações entregues / total. `fallbackRatePercent` = `EMAIL_FALLBACK` / notificações que tentaram WhatsApp.
- `topErrors`: até 5 mensagens de erro mais frequentes; erros JSON da Graph API/SendGrid são reduzidos ao campo `message` (máx. 150 caracteres).
- Taxas com 1 casa; `null` quando não há base (zero notificações).

**CSV** (`/export`): resposta `text/csv; charset=UTF-8` com `Content-Disposition: attachment` (`bens-por-categoria.csv` / `ordens-de-servico.csv`). Formato pensado para o Excel pt-BR: separador `;`, UTF-8 com BOM, cabeçalhos e rótulos de status em português, datas `dd/MM/yyyy HH:mm` (fuso local), custo com vírgula (`25,50`). Textos que começam com `=`, `+`, `-` ou `@` recebem um apóstrofo na frente (proteção contra injeção de fórmulas). Para baixar no frontend, use `fetch` com o header `Authorization` e trate a resposta como `blob` (um `<a href>` simples não envia o token).

**PDF** (`/export/pdf`, desde 2026-10-07): mesma ideia do CSV — `GET .../export/pdf` ao lado de cada `GET .../export` existente, resposta `application/pdf` com `Content-Disposition: attachment` (ex.: `ordens-de-servico.pdf`). Gerado inteiramente em memória a partir dos mesmos dados do CSV (tabela com cabeçalho azul, título do relatório e timestamp de geração); **nada é salvo no backend** — o PDF não fica em disco nem no banco, só existe no corpo dessa resposta. Para baixar de novo, o frontend chama o endpoint de novo. Relatórios com muitas colunas (ex.: OS, 12 colunas) saem em paisagem automaticamente; o relatório de auditoria em PDF trás só os 300 eventos mais recentes do filtro (o CSV continua trazendo até 10.000 — PDF é para leitura, não dump de dados). Mesmos filtros de query string do `/export` correspondente.

### 6.17 Webhook do WhatsApp — `/api/v1/whatsapp/webhook` (não é para o frontend consumir)

Documentado aqui só por completude — é chamado pela Meta (Graph API), não pelo frontend. Não requer JWT (liberado no `SecurityConfig`); a segurança é feita por assinatura própria.

| Método | Path | Auth | Descrição |
|---|---|---|---|
| GET | `/` | Público | Desafio de verificação do webhook (`hub.mode`, `hub.verify_token`, `hub.challenge`) usado pela Meta ao configurar a integração |
| POST | `/` | Público, validado por `X-Hub-Signature-256` (HMAC-SHA256) | Recebe eventos de mensagem (`messages`) e de status de entrega (`statuses`) |

Desde 2026-10-02, toda mensagem de texto recebida (`messages`) é interpretada (RF15): se estiver no formato
```
Abrir OS
Setor: <nome do setor>
Bem: <tombamento, opcional>
Problema: <descrição>
```
o backend abre a OS de fato (`channel: WHATSAPP`, ver 6.10) e responde com a confirmação; se o setor/bem não existir, responde com o erro explicando o formato. Desde 2026-10-02, a mensagem `Status` (lista os últimos chamados do telefone) ou `Status #<número>` (detalhe de um chamado) também é interpretada (RF16) — só mostra o chamado se ele pertencer ao telefone que perguntou; desde 2026-11-02, `Nota #<número> <1 a 5>` registra a avaliação do atendimento de uma OS concluída do próprio telefone (uma vez só; o convite vai na notificação de conclusão enviada ao solicitante); qualquer outra mensagem recebe a mensagem de boas-vindas com os formatos. Não há nada aqui que o frontend precise chamar ou exibir — é tudo mensagem de WhatsApp, não API.

## 7. O que NÃO existe ainda (não tente consumir)

Para o frontend não implementar telas para funcionalidades inexistentes:

- **Não existe endpoint `/notifications`** para consultar o que foi enviado (nem por e-mail nem por WhatsApp) — o registro fica só em `tb_notifications`, sem endpoint de leitura.
- O dashboard (`GET /api/v1/dashboard`) **não** traz agregação por categoria de bem — use `GET /reports/assets`.
- **Sem baixa patrimonial estruturada**: retirar um bem do acervo usa o mecanismo genérico de soft delete (`DELETE /api/v1/assets/{id}`), sem campo de motivo/data/responsável pela baixa.
- **Sem reabertura de OS**: não é possível voltar uma OS para `OPEN` nem alterar uma OS `RESOLVED`/`CANCELLED` (ver `PATCH /{id}/status`, 6.10).
- **Sem `@PreAuthorize` por dono do recurso**: qualquer usuário autenticado pode ler qualquer OS/ativo/etc. de qualquer solicitante — a restrição de papel (seção 4) é por **tipo de operação**, não por "isso é meu".

## 8. Referências

- Código-fonte: `../src/main/java/br/com/fpo/sgc` (organização por domínio: `entity/repository/service+impl/controller/dto/mapper`).
- Migrations: `src/main/resources/db/migration/V1` a `V20`.
- Roadmap completo, decisões de arquitetura e status detalhado por requisito funcional: `PLANO_IMPLEMENTACAO_MVP.md` (raiz deste repositório).
- Swagger UI ao vivo: `GET /swagger-ui.html` no ambiente rodando.
