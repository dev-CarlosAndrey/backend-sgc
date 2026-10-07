# Diagramas UML

> Exigido pelas diretrizes de TCC da FPO para a modalidade III (MVP): "documentação técnica: elaboração de [...] diagramas (incluindo UML) [...]" (`docs/apoio/Orientações de TCC para ADS -2026.pdf`). Os diagramas abaixo foram produzidos a partir do código efetivamente implementado (entidades em `src/main/java/br/com/fpo/sgc/domain/**`, migrações em `src/main/resources/db/migration/V1`–`V19`), não do desenho conceitual original do TCC — para a divergência entre os dois, ver `PLANO_IMPLEMENTACAO_MVP.md`, seção 0.1.
>
> Formato: [Mermaid](https://mermaid.js.org/), renderizado automaticamente pelo GitHub/GitLab ao visualizar este arquivo, e editável como texto (sem depender de ferramenta gráfica externa).

## 1. Diagrama de casos de uso

Mermaid não tem um tipo de diagrama nativo para caso de uso UML; a representação abaixo usa um grafo para mostrar quais atores acionam quais casos de uso, mantendo a semântica (ator → caso de uso, `<<include>>` para pré-condição).

```mermaid
flowchart LR
    Admin([Administrador])
    Tecnico([Técnico])
    Solicitante([Solicitante])
    Meta([WhatsApp / Meta Cloud API])

    subgraph UC_Patrimonio[Controle Patrimonial]
        UC01[Cadastrar/editar/inativar bem]
        UC02[Consultar bens e histórico de manutenção]
        UC03[Registrar movimentação entre setores]
    end

    subgraph UC_OS[Ordens de Serviço]
        UC04[Abrir OS pelo painel]
        UC05[Abrir OS por WhatsApp]
        UC06[Atribuir técnico]
        UC07[Mudar status da OS]
        UC08[Encerrar OS com laudo]
        UC09[Consultar status por WhatsApp]
        UC10[Consultar OS pelo painel]
    end

    subgraph UC_Relatorios[Relatórios e Painel]
        UC11[Visualizar dashboard]
        UC12[Exportar relatório CSV/PDF]
    end

    subgraph UC_Cadastro[Cadastros Mestres]
        UC13[Gerenciar usuários/empresas/setores/categorias]
    end

    Admin --> UC01
    Admin --> UC02
    Admin --> UC03
    Admin --> UC04
    Admin --> UC06
    Admin --> UC07
    Admin --> UC08
    Admin --> UC10
    Admin --> UC11
    Admin --> UC12
    Admin --> UC13

    Tecnico --> UC02
    Tecnico --> UC07
    Tecnico --> UC08
    Tecnico --> UC10

    Solicitante --> UC02
    Solicitante --> UC04
    Solicitante --> UC10

    Meta -. mensagem recebida .-> UC05
    Meta -. mensagem recebida .-> UC09
    UC05 -. inclui .-> UC_Login[Resolver/criar usuário por telefone]
    UC04 -. inclui .-> UC_Login2[Autenticar via JWT]
```

**Notas de leitura**:
- `Solicitante` é qualquer usuário autenticado sem papel de administrador/técnico (`Role.USER`); quem abre OS pelo WhatsApp (UC05) não precisa de cadastro prévio — o usuário é resolvido/criado automaticamente pelo número de telefone (`UserService.findOrCreateByPhone`), daí a relação `<<include>>` com um caso de uso diferente do login tradicional.
- `UC09` (consultar status por WhatsApp) **não** cria usuário — telefone desconhecido recebe apenas "sem histórico" (ver `PLANO_IMPLEMENTACAO_MVP.md`, RF16).

## 2. Diagrama de classes (domínio)

Representa as entidades JPA efetivamente implementadas (`@Entity`) e seus relacionamentos. Atributos omitidos quando óbvios pelo nome; tipos de enum anotados entre parênteses. A maioria das entidades estende `BaseEntity` (`createdAt`, `updatedAt`, `active`), omitido em cada classe para não poluir o diagrama — exceções: `Company`, `Building` e `Brand` têm um `active` próprio (sem `createdAt`/`updatedAt`), e `AuditLog` não estende `BaseEntity`, mantendo apenas seu próprio `createdAt` (campo central de um log imutável, sem `updatedAt`/`active`).

```mermaid
classDiagram
    class User {
        +Long id
        +String name
        +String email
        +String password
        +String phone
        +Role role
    }
    class Company {
        +Long id
        +String companyName
        +String cnpj
        +String logoUrl
    }
    class Building {
        +Long id
        +String name
        +String address
    }
    class Sector {
        +Long id
        +String name
    }
    class Employee {
        +Long id
        +String name
        +String cpf
        +String registrationNumber
    }
    class AssetCategory {
        +Long id
        +String name
    }
    class Asset {
        +Long id
        +String tagNumber
        +AssetStatus status
        +AssetConservationState conservationState
        +LocalDate acquisitionDate
        +BigDecimal acquisitionValue
        +String photoUrl
    }
    class AssetMovement {
        +Long id
        +String reason
        +LocalDate movementDate
    }
    class ServiceOrder {
        +Long id
        +String title
        +String description
        +ServiceOrderPriority priority
        +ServiceOrderStatus status
        +ServiceOrderChannel channel
        +String solutionDetails
        +BigDecimal cost
        +Instant closedAt
    }
    class ServiceOrderStatusLog {
        +Long id
        +ServiceOrderStatus previousStatus
        +ServiceOrderStatus newStatus
        +String changedBy
        +String note
    }
    class Notification {
        +Long id
        +String channel
        +String deliveryStatus
        +int attempt
        +String errorMessage
    }
    class AuditLog {
        +Long id
        +String entityName
        +Long entityId
        +String action
        +String performedBy
        +String details
    }
    class Brand { +Long id +String name }
    class Material { +Long id +String name }
    class Product { +Long id +String name }

    Company "1" --> "0..*" Building
    Building "1" --> "0..*" Sector
    Sector "1" --> "0..*" Employee
    Sector "0..1" --> "0..1" Employee : manager
    Company "1" --> "0..*" Employee
    Sector "1" --> "0..*" Asset
    AssetCategory "0..1" --> "0..*" Asset
    Employee "0..1" --> "0..*" Asset : responsible (carga patrimonial)
    Asset "1" --> "0..*" AssetMovement
    Sector "1" --> "0..*" AssetMovement : origin/destination
    Asset "0..1" --> "0..*" ServiceOrder
    Sector "1" --> "0..*" ServiceOrder
    User "1" --> "0..*" ServiceOrder : requester
    User "0..1" --> "0..*" ServiceOrder : technician
    ServiceOrder "1" --> "0..*" ServiceOrderStatusLog
    ServiceOrder "1" --> "0..*" Notification
    User "1" --> "0..*" Notification : recipient
    Brand "1" --> "0..*" Product
    Material "1" --> "0..*" Product
```

## 3. Diagrama entidade-relacionamento (ER)

Simplificado ao nível de tabela/chave, refletindo o schema aplicado pelas migrações Flyway (`V1`–`V19`). Para a lista completa de colunas, consultar `src/main/resources/db/migration/`.

```mermaid
erDiagram
    tb_companies ||--o{ tb_buildings : has
    tb_buildings ||--o{ tb_sectors : has
    tb_sectors ||--o{ tb_employees : has
    tb_companies ||--o{ tb_employees : has
    tb_sectors |o--o| tb_employees : "manager_employee_id"
    tb_sectors ||--o{ tb_assets : has
    tb_asset_categories |o--o{ tb_assets : categorizes
    tb_employees |o--o{ tb_assets : "responsible_employee_id"
    tb_assets ||--o{ tb_asset_movements : has
    tb_assets |o--o{ tb_service_orders : "asset_id"
    tb_sectors ||--o{ tb_service_orders : has
    tb_users ||--o{ tb_service_orders : "requester_id"
    tb_users |o--o{ tb_service_orders : "technician_id"
    tb_service_orders ||--o{ tb_service_order_status_logs : has
    tb_service_orders ||--o{ tb_notifications : generates
    tb_brands ||--o{ tb_products : has
    tb_materials ||--o{ tb_products : has

    tb_users {
        bigint id PK
        varchar name
        varchar email UK
        varchar password
        varchar phone
        varchar role
        boolean active
    }
    tb_companies {
        bigint id PK
        varchar company_name
        varchar cnpj UK
    }
    tb_buildings {
        bigint id PK
        bigint company_id FK
        varchar name
        varchar address
    }
    tb_sectors {
        bigint id PK
        bigint building_id FK
        bigint manager_employee_id FK
        varchar name
    }
    tb_employees {
        bigint id PK
        bigint company_id FK
        bigint sector_id FK
        varchar name
        varchar cpf UK
        varchar registration_number
    }
    tb_asset_categories {
        bigint id PK
        varchar name UK
    }
    tb_assets {
        bigint id PK
        varchar tag_number UK
        bigint sector_id FK
        bigint category_id FK
        bigint responsible_employee_id FK
        varchar status
        varchar conservation_state
        date acquisition_date
        decimal acquisition_value
        varchar photo_url
    }
    tb_asset_movements {
        bigint id PK
        bigint asset_id FK
        bigint origin_sector_id FK
        bigint destination_sector_id FK
        varchar reason
        date movement_date
    }
    tb_service_orders {
        bigint id PK
        bigint asset_id FK
        bigint sector_id FK
        bigint requester_id FK
        bigint technician_id FK
        varchar title
        varchar priority
        varchar status
        varchar channel
        decimal cost
        timestamp closed_at
    }
    tb_service_order_status_logs {
        bigint id PK
        bigint service_order_id FK
        varchar previous_status
        varchar new_status
        varchar changed_by
        varchar note
    }
    tb_notifications {
        bigint id PK
        bigint service_order_id FK
        bigint recipient_id FK
        varchar channel
        varchar delivery_status
        int attempt
        varchar error_message
    }
    tb_audit_logs {
        bigint id PK
        varchar entity_name
        bigint entity_id
        varchar action
        varchar performed_by
        timestamp created_at
    }
    tb_brands {
        bigint id PK
        varchar name
    }
    tb_materials {
        bigint id PK
        varchar name
    }
    tb_products {
        bigint id PK
        bigint brand_id FK
        bigint material_id FK
        varchar name
    }
```

## 4. Diagrama de sequência — abertura de OS por WhatsApp (RF15)

Fluxo do diferencial central do produto: uma mensagem recebida vira uma Ordem de Serviço registrada no banco, sem exigir cadastro prévio do solicitante.

```mermaid
sequenceDiagram
    actor U as Usuário (WhatsApp)
    participant Meta as Meta Cloud API
    participant WH as WhatsAppWebhookController
    participant Reply as WhatsAppAutoReplyService
    participant Cmd as WhatsAppServiceOrderCommand
    participant Intake as WhatsAppServiceOrderIntakeService
    participant UserSvc as UserService
    participant OSSvc as ServiceOrderService
    participant DB as MySQL

    U->>Meta: "Setor: Financeiro\nProblema: projetor sem imagem"
    Meta->>WH: POST /api/v1/whatsapp/webhook (assinado X-Hub-Signature-256)
    WH->>WH: valida assinatura (WhatsAppSignatureVerifier)
    WH->>Reply: handleIncomingMessage() [@Async]
    Reply->>Cmd: parse(texto)
    Cmd-->>Reply: comando reconhecido (Setor, Bem?, Problema)
    Reply->>Intake: open(comando, telefone)
    Intake->>DB: SectorRepository.findByNameIgnoreCaseAndActiveTrue
    DB-->>Intake: Sector encontrado
    Intake->>UserSvc: findOrCreateByPhone(telefone, nomeContato)
    UserSvc->>DB: busca por phone
    alt usuário não existe
        UserSvc->>DB: cria User (role=USER, e-mail/senha sintéticos)
    end
    UserSvc-->>Intake: User (solicitante)
    Intake->>OSSvc: create(dto, ServiceOrderChannel.WHATSAPP)
    OSSvc->>DB: INSERT tb_service_orders (channel=WHATSAPP)
    OSSvc->>DB: INSERT tb_service_order_status_logs
    OSSvc-->>Intake: ServiceOrder criada (id)
    Intake-->>Reply: confirmação (nº da OS)
    Reply->>Meta: POST /{phone-number-id}/messages (confirmação)
    Meta->>U: "Chamado #42 aberto com sucesso."
```

## 5. Diagrama de sequência — notificação com fallback WhatsApp → e-mail

Mecanismo de robustez: nenhuma mudança de status de OS fica sem notificar o envolvido, mesmo que o canal preferencial (WhatsApp) falhe.

```mermaid
sequenceDiagram
    participant OSSvc as ServiceOrderServiceImpl
    participant Spring as Spring Event Bus
    participant Listener as ServiceOrderNotificationListener
    participant Notif as NotificationServiceImpl
    participant WA as WhatsAppNotificationSender
    participant Email as EmailNotificationSender
    participant DB as MySQL (tb_notifications)

    OSSvc->>Spring: publica ServiceOrderEvent (RESOLVED, actorUserId)
    Note over OSSvc,Spring: publicado dentro da transação;<br/>listener só dispara após commit
    Spring->>Listener: @TransactionalEventListener(AFTER_COMMIT) [@Async]
    Listener->>Notif: notify(solicitante, mensagem, status)
    Notif->>WA: send(telefone, mensagem)
    alt WhatsApp indisponível (token expirado, fora da janela 24h, etc.)
        WA-->>Notif: falha (erro da Graph API)
        Notif->>DB: registra tentativa (channel=WHATSAPP, status=FAILED)
        Notif->>Email: send(email, mensagem, status)
        Email-->>Notif: sucesso
        Notif->>DB: registra tentativa (channel=EMAIL, status=SENT)
    else WhatsApp disponível
        WA-->>Notif: sucesso
        Notif->>DB: registra tentativa (channel=WHATSAPP, status=SENT)
    end
    Note over Notif: falha de notificação nunca propaga exceção<br/>para a operação de negócio que originou o evento
```
