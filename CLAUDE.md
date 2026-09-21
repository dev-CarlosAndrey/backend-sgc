# CLAUDE.md

Este arquivo fornece orientações para o Claude Code (claude.ai/code) ao trabalhar com código neste repositório.

## Visão geral do projeto

SGC API é um backend REST em Spring Boot 4.1 / Java 21 (`br.com.fpo.sgc`) para gestão de ativos/inventário: ativos (assets), materiais, produtos, ordens de serviço, funcionários, prédios/setores, empresas, marcas, log de auditoria e autenticação via JWT. A persistência é MySQL via Spring Data JPA, com o Flyway gerenciando as migrações de schema. Comentários do código de domínio e mensagens de exceção são escritos em português (pt-BR); mantenha essa convenção ao alterar módulos existentes.

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
- `EVOLUTION_API_URL`, `EVOLUTION_API_KEY`, `EVOLUTION_INSTANCE_NAME` — configuração de integração com a Evolution API (WhatsApp), declarada mas ainda não conectada a nenhum bean/cliente no código.
- `PORT` — porta do servidor, padrão `8080`.

`spring.jpa.hibernate.ddl-auto` está como `validate` — o mapeamento das entidades JPA precisa já corresponder ao schema gerado pelas migrações do Flyway. Mudanças de schema devem passar por uma nova migração do Flyway, e não por `ddl-auto`.

A UI do Swagger/OpenAPI é exposta em `/swagger-ui.html` (springdoc), sem autenticação, conforme `SecurityConfig`.

## Arquitetura

### Organização dos pacotes

- `br.com.fpo.sgc.common` — responsabilidades transversais compartilhadas por todos os domínios:
  - `entity.BaseEntity` — `@MappedSuperclass` que fornece `createdAt`/`updatedAt`/`active`, preenchidos automaticamente via `@PrePersist`/`@PreUpdate`. Todas as entidades JPA estendem essa classe.
  - `exception` — `BusinessException` (violações de regra de negócio) e `GlobalExceptionHandler`, um `@RestControllerAdvice` que mapeia `EntityNotFoundException` → 404, `BusinessException` → 400 e `MethodArgumentNotValidException` → 422, todos como respostas `ProblemDetail` no formato RFC 7807, com um `type` (URI) e propriedade `timestamp`.
  - `security` — autenticação JWT stateless: `SecurityConfig` (filter chain, CORS, libera `/api/v1/auth/login` e os caminhos do Swagger, exige autenticação para o resto), `SecurityFilter` (lê `Authorization: Bearer <token>`, valida e popula o `SecurityContextHolder`), `TokenService` (auth0 java-jwt, HS256, expiração de 8h, issuer `sgc-api`, claim `role`), `UserDetailsServiceCustom`.
  - `config.OpenApiConfig` — configuração dos beans do springdoc/OpenAPI.
- `br.com.fpo.sgc.domain.<nome>` — um pacote por entidade de negócio (`asset`, `asset.movement`, `audit`, `brand`, `company`, `dashboard`, `employee`, `location.building`, `location.sector`, `material`, `product`, `serviceorder`, `user`). Cada um segue a mesma estrutura interna:
  - `<Entity>.java` — entidade JPA, estende `BaseEntity`, usa Lombok `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`.
  - `<Entity>Repository.java` — interface de repositório Spring Data JPA; finders customizados seguem a convenção de soft-delete do módulo (ex.: `findByActiveTrue`, `findBySectorIdAndActiveTrue`).
  - `<Entity>Service.java` / `<Entity>ServiceImpl.java` — separação entre interface e implementação. É na implementação do service que ficam as regras de negócio, os lançamentos de `EntityNotFoundException`/`BusinessException` e o registro de auditoria. Métodos de leitura usam `@Transactional(readOnly = true)`, métodos que alteram dados usam `@Transactional`.
  - `<Entity>Controller.java` — `@RestController` enxuto sob `/api/v1/<recurso>`; delega diretamente para a interface do service, sem lógica no controller.
  - `dto/<Entity>RequestDTO.java`, `dto/<Entity>ResponseDTO.java` — records Java; DTOs de request carregam anotações `jakarta.validation`.
  - `mapper/<Entity>Mapper.java` — mapper `@Component` escrito manualmente entre entidade e DTOs (sem MapStruct); constrói entidades via builder do Lombok e DTOs via construtor do record. Quando um mapper precisa do mapper de uma entidade relacionada (ex.: `AssetMapper` precisando de `SectorMapper`), ele é injetado via construtor (`@RequiredArgsConstructor`).

Novos módulos de domínio devem seguir exatamente essa estrutura (entity/repository/service+impl/controller/dto/mapper) em vez de introduzir uma camadas diferente.

### Convenções entre domínios

- **Soft delete**: entidades nunca são excluídas de fato (hard delete) nos fluxos CRUD comuns. O `delete()` nas implementações de service seta `active = false` e salva, e as consultas de listagem/busca múltipla filtram por `active = true` (métodos de repositório nomeados de acordo, ex.: `findByActiveTrue`).
- **Log de auditoria**: operações que alteram dados nas implementações de service (create/update/delete) chamam `AuditLogService.log(entityName, entityId, action, performedBy, details)` depois que a alteração é persistida. Os registros de `AuditLog` são gravados dentro da mesma transação da alteração. Ao adicionar create/update/delete em um service novo ou existente, inclua a chamada correspondente de log de auditoria.
- **Exceções**: lance `jakarta.persistence.EntityNotFoundException` para buscas de recurso não encontrado e `br.com.fpo.sgc.common.exception.BusinessException` para violações de regra (chaves duplicadas, transições de estado inválidas, etc.) — não crie novos tipos de exceção nem retorne corpos de erro manualmente pelos controllers; o `GlobalExceptionHandler` cuida da tradução para respostas HTTP.
- **Paginação**: endpoints de listagem recebem `Pageable` via `@PageableDefault(size = 10, sort = "...")` e retornam `Page<...ResponseDTO>`.
- **Migrações de banco**: os arquivos do Flyway ficam em `src/main/resources/db/migration`, nomeados como `V<n>__<descrição>.sql`, estritamente sequenciais (atualmente V1–V18). Nunca edite uma migração já aplicada; adicione um novo arquivo `V<n+1>__...sql`.
- **Autenticação/papéis**: o enum `Role` (`ADMIN`, `TECHNICIAN`, `USER`) fica em `User` e é embutido como claim no JWT; a restrição de acesso por papel a nível de endpoint ainda não está implementada no `SecurityConfig` além de autenticado-vs-permitAll, então não assuma que existe `@PreAuthorize` a nível de método a menos que você veja isso adicionado.
