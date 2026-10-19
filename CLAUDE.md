# CLAUDE.md

Este arquivo fornece orientações para o Claude Code (claude.ai/code) ao trabalhar com código neste repositório.

## Visão geral do projeto

SGC API é um backend REST em Spring Boot 4.1 / Java 21 (`br.com.fpo.sgc`) para gestão de ativos/inventário: ativos (assets) e suas categorias, materiais, produtos, ordens de serviço (com histórico de status), funcionários, prédios/setores, empresas, marcas, log de auditoria, notificações por e-mail, relatórios (JSON/CSV) e autenticação via JWT. A persistência é MySQL via Spring Data JPA, com o Flyway gerenciando as migrações de schema. Comentários do código de domínio e mensagens de exceção são escritos em português (pt-BR); mantenha essa convenção ao alterar módulos existentes.

## Comandos

Compilar o projeto:
```
./mvnw compile
```

Rodar a suíte de testes completa:
```
./mvnw test
```

Rodar uma única classe ou método de teste:
```
./mvnw test -Dtest=SgcApiApplicationTests
./mvnw test -Dtest=SgcApiApplicationTests#contextLoads
```

Rodar a aplicação localmente (requer uma instância MySQL acessível, ver abaixo):
```
./mvnw spring-boot:run
```

Empacotar um jar executável:
```
./mvnw package
```

### Ambiente local / configuração

`src/main/resources/application.yml` lê toda a configuração de infraestrutura a partir de variáveis de ambiente, com valores padrão para desenvolvimento:
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` — conexão MySQL (padrão aponta para `localhost:3306/sgc_db`, usuário/senha `root`/`root`).
- `JWT_SECRET` — segredo HMAC para assinatura do JWT (`api.security.token.secret`).
- `SENDGRID_API_KEY`, `SENDGRID_FROM_EMAIL` (e opcionais `SENDGRID_BASE_URL`, `SENDGRID_FROM_NAME`) — envio de e-mail das notificações. Sem `SENDGRID_API_KEY` (ela precisa começar com `SG.`) o canal de e-mail fica desabilitado e a aplicação segue funcionando normalmente. **A chave é segredo: só por variável de ambiente (ex.: run configuration do IntelliJ, que fica no `.idea/` ignorado pelo Git); nunca a coloque em arquivo versionado, em `application.yml` nem em mensagem de commit.**
- `NOTIFICATION_FALLBACK_ENABLED`, `NOTIFICATION_MAX_ATTEMPTS`, `NOTIFICATION_RETRY_BACKOFF_MS` — política de fallback e retentativa das notificações (padrão: `true`, 3 tentativas, 1000 ms).
- `WHATSAPP_CLOUD_API_BASE_URL`, `WHATSAPP_ACCESS_TOKEN`, `WHATSAPP_PHONE_NUMBER_ID`, `WHATSAPP_BUSINESS_ACCOUNT_ID`, `WHATSAPP_WEBHOOK_VERIFY_TOKEN`, `WHATSAPP_APP_SECRET` — WhatsApp Cloud API (Meta), que **substitui** a Evolution API (removida do `application.yml`). Declaradas, mas **ainda não lidas por nenhum bean**: a integração (Fase 2) não existe.
- `UPLOAD_DIR` — diretório de uploads (padrão `uploads`), servido em `/uploads/**`.
- `PORT` — porta do servidor, padrão `8080`.

`spring.jpa.hibernate.ddl-auto` está como `validate` — o mapeamento das entidades JPA precisa já corresponder ao schema gerado pelas migrações do Flyway. Mudanças de schema devem passar por uma nova migração do Flyway, e não por `ddl-auto`.

A UI do Swagger/OpenAPI é exposta em `/swagger-ui.html` (springdoc), sem autenticação, conforme `SecurityConfig`.

## Arquitetura

### Organização dos pacotes

- `br.com.fpo.sgc.common` — responsabilidades transversais compartilhadas por todos os domínios:
  - `entity.BaseEntity` — `@MappedSuperclass` que fornece `createdAt`/`updatedAt`/`active`, preenchidos automaticamente via `@PrePersist`/`@PreUpdate`. Todas as entidades JPA estendem essa classe.
  - `exception` — `BusinessException` (violações de regra de negócio) e `GlobalExceptionHandler`, um `@RestControllerAdvice` que mapeia `EntityNotFoundException` → 404, `BusinessException` → 400 e `MethodArgumentNotValidException` → 422, `MethodArgumentTypeMismatchException` (data/enum inválido em parâmetro) e `MaxUploadSizeExceededException` → 400, todos como respostas `ProblemDetail` no formato RFC 7807, com um `type` (URI) e propriedade `timestamp`.
  - `security` — autenticação JWT stateless: `SecurityConfig` (filter chain, CORS, libera `/api/v1/auth/login` e os caminhos do Swagger, exige autenticação para o resto), `SecurityFilter` (lê `Authorization: Bearer <token>`, valida e popula o `SecurityContextHolder`), `TokenService` (auth0 java-jwt, HS256, expiração de 8h, issuer `sgc-api`, claim `role`), `UserDetailsServiceCustom`.
  - `config` — `OpenApiConfig` (springdoc/OpenAPI), `WebConfig` e `AsyncConfig` (`@EnableAsync`, usado pelas notificações).
  - `storage.FileStorageService` — upload de arquivos para `uploads/<subdir>/<uuid>.<ext>`.
  - `util` — `PeriodRange` (converte datas inclusivas `LocalDate` em `[from, to)` de `Instant` no fuso `America/Fortaleza`; use-o em qualquer filtro por período) e `CsvWriter` (CSV para Excel pt-BR: `;`, UTF-8 com BOM, neutraliza CSV injection).
- `br.com.fpo.sgc.domain.<nome>` — um pacote por entidade de negócio (`asset`, `asset.category`, `asset.movement`, `audit`, `brand`, `company`, `dashboard`, `employee`, `location.building`, `location.sector`, `material`, `notification`, `product`, `report`, `serviceorder`, `serviceorder.statuslog`, `user`). Cada um segue a mesma estrutura interna:
  - `<Entity>.java` — entidade JPA, estende `BaseEntity`, usa Lombok `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`.
  - `<Entity>Repository.java` — interface de repositório Spring Data JPA; finders customizados seguem a convenção de soft-delete do módulo (ex.: `findByActiveTrue`, `findBySectorIdAndActiveTrue`).
  - `<Entity>Service.java` / `<Entity>ServiceImpl.java` — separação entre interface e implementação. É na implementação do service que ficam as regras de negócio, os lançamentos de `EntityNotFoundException`/`BusinessException` e o registro de auditoria. Métodos de leitura usam `@Transactional(readOnly = true)`, métodos que alteram dados usam `@Transactional`.
  - `<Entity>Controller.java` — `@RestController` enxuto sob `/api/v1/<recurso>`; delega diretamente para a interface do service, sem lógica no controller.
  - `dto/<Entity>RequestDTO.java`, `dto/<Entity>ResponseDTO.java` — records Java; DTOs de request carregam anotações `jakarta.validation`.
  - `mapper/<Entity>Mapper.java` — mapper `@Component` escrito manualmente entre entidade e DTOs (sem MapStruct); constrói entidades via builder do Lombok e DTOs via construtor do record. Quando um mapper precisa do mapper de uma entidade relacionada (ex.: `AssetMapper` precisando de `SectorMapper`), ele é injetado via construtor (`@RequiredArgsConstructor`).

Novos módulos de domínio devem seguir exatamente essa estrutura (entity/repository/service+impl/controller/dto/mapper) em vez de introduzir uma organização em camadas diferente. Exceções que não são CRUD de entidade: `dashboard` e `report` só agregam dados (sem entidade própria), e `notification` é infraestrutura orientada a eventos (ver abaixo).

### Ordens de serviço, notificações e relatórios

- **Toda mudança de status de OS** (`create`, `assignTechnician`, `changeStatus`, `resolveOrder`) grava um `ServiceOrderStatusLog` (`tb_service_order_status_logs`, via `logStatusChange`) **e** publica um `ServiceOrderEvent`. Ao criar um novo fluxo que mude o status, faça as duas coisas. Regras: `RESOLVED` só via `/resolve` (exige laudo); `IN_PROGRESS` exige técnico; OS `RESOLVED`/`CANCELLED` não aceitam nova mudança nem resolução.
- **Notificações**: `ServiceOrderNotificationListener` (`@Async` + `@TransactionalEventListener(AFTER_COMMIT)`) notifica solicitante, técnico atribuído e, na resolução, quem resolveu (`actorUserId`), sem duplicar e ignorando inativos. Falha de notificação nunca afeta a operação da OS. `NotificationService` tenta WhatsApp (com retentativas) e cai para e-mail, gravando cada tentativa em `tb_notifications`; ele **não é `@Transactional` de propósito** (o registro de falha não pode sofrer rollback e não se mantém transação aberta durante chamadas HTTP/backoff). Novos canais implementam `NotificationSender`. Hoje só existe `EmailNotificationSender` (SendGrid, via `RestClient`, HTML por `ServiceOrderEmailTemplate` com cor/rótulo/barra de progresso por status). **Não há `WhatsAppNotificationSender`.**
- **Relatórios** (`/api/v1/reports`, só `ADMIN`): agregação em memória sobre `ServiceOrderRepository.findForReport` (filtros opcionais, fetch join) e consultas agrupadas de `AssetRepository`; exportação CSV em `/export`.

### Convenções entre domínios

- **Soft delete**: entidades nunca são excluídas de fato (hard delete) nos fluxos CRUD comuns. O `delete()` nas implementações de service seta `active = false` e salva, e as consultas de listagem/busca múltipla filtram por `active = true` (métodos de repositório nomeados de acordo, ex.: `findByActiveTrue`).
- **Log de auditoria**: operações que alteram dados nas implementações de service (create/update/delete) chamam `AuditLogService.log(entityName, entityId, action, performedBy, details)` depois que a alteração é persistida. Os registros de `AuditLog` são gravados dentro da mesma transação da alteração. Ao adicionar create/update/delete em um service novo ou existente, inclua a chamada correspondente de log de auditoria. O parâmetro `performedBy` é **ignorado**: o responsável gravado é sempre o usuário autenticado (`SecurityContextHolder`), ou `SYSTEM` sem autenticação.
- **Exceções**: lance `jakarta.persistence.EntityNotFoundException` para buscas de recurso não encontrado e `br.com.fpo.sgc.common.exception.BusinessException` para violações de regra (chaves duplicadas, transições de estado inválidas, etc.) — não crie novos tipos de exceção nem retorne corpos de erro manualmente pelos controllers; o `GlobalExceptionHandler` cuida da tradução para respostas HTTP.
- **Paginação**: endpoints de listagem recebem `Pageable` via `@PageableDefault(size = 10, sort = "...")` (alguns usam 20) e retornam `Page<...ResponseDTO>`. O `Page` é serializado com `PageSerializationMode.VIA_DTO` (`SgcApiApplication`): a resposta é `{ content: [...], page: { size, number, totalElements, totalPages } }`, sem `first`/`last`/`sort`. Os `GET /` de listagem aceitam busca `q` e filtros opcionais: o controller recebe `@RequestParam(required = false)`, o service converte `q` com `SearchTerm.toLikePattern` e o repositório tem um método `search(...)` em JPQL no estilo `(:param IS NULL OR ...)` com `LOWER(campo) LIKE :q ESCAPE '!'`. Siga esse padrão ao criar uma listagem nova e documente os filtros no contrato.
- **Migrações de banco**: os arquivos do Flyway ficam em `src/main/resources/db/migration`, nomeados como `V<n>__<descrição>.sql`, estritamente sequenciais (atualmente V1–V19). Nunca edite uma migração já aplicada; adicione um novo arquivo `V<n+1>__...sql`.
- **Autenticação/papéis**: o enum `Role` (`ADMIN`, `TECHNICIAN`, `USER`) fica em `User` e é embutido como claim no JWT. A restrição por papel é feita com `@PreAuthorize` nos métodos dos controllers (`@EnableMethodSecurity` no `SecurityConfig`). Política: escritas em cadastros mestres (usuários, empresas, prédios, setores, funcionários, marcas, materiais, produtos, categorias, ativos, movimentações) → `ADMIN`; leituras → qualquer autenticado; OS: abrir → qualquer autenticado, atribuir técnico/excluir → `ADMIN`, resolver/alterar status → `ADMIN` ou `TECHNICIAN`; dashboard e relatórios → `ADMIN`. Novos endpoints de escrita devem declarar o `@PreAuthorize` correspondente. Não há restrição por dono do recurso. O login (`POST /api/v1/auth/login`) usa o campo `name` do usuário como `username`, não o e-mail. A migration `V16` cria o admin de bootstrap (`admin` / `Admin@123`) — trocar a senha fora de dev.

## Testes

- `./mvnw test` precisa de MySQL acessível: `SgcApiApplicationTests#contextLoads` sobe o contexto completo (valida o Flyway e o `ddl-auto: validate`). Os demais testes são unitários com Mockito, sem banco.
- Consultas JPQL novas **não** são exercitadas pelos testes unitários; valide-as com a aplicação rodando contra o MySQL.
- Services com muitas dependências são instanciados direto no teste (construtor com `null` nas que não interessam). `EmailNotificationSender` tem um construtor de pacote que recebe `RestClient.Builder` para uso com `MockRestServiceServer` (o Boot 4 não fornece um `RestClient.Builder` como bean sem o starter específico).

## Documentos de referência (mantenha atualizados)

- `docs/API_CONTRATO_FRONTEND.md` — contrato da API para o frontend. Atualize ao criar/alterar endpoint, DTO, regra de acesso ou formato de erro.
- `docs/PLANO_IMPLEMENTACAO_MVP.md` — roadmap, status por requisito (RF) e decisões (ex.: WhatsApp Cloud API no lugar da Evolution API, seção 0.2). Atualize o status ao concluir um item.
