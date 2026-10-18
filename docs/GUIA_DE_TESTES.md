# Guia de Testes

> Documenta a etapa de **testes** do ciclo de vida do sistema, exigida pelas diretrizes de TCC da FPO para a modalidade III (MVP): "demonstração do domínio das etapas de levantamento de requisitos, modelagem, implementação, **testes** e implantação" (`docs/apoio/Orientações de TCC para ADS -2026.pdf`).

## 1. Estratégia de testes

O backend usa dois tipos de teste, por propósito distinto:

1. **Testes unitários (Mockito, sem banco)** — cobrem regras de negócio dos services, mapeadores e utilitários, com dependências substituídas por mocks ou instanciadas diretamente (`new Service(mock1, mock2, ...)` — ver convenção descrita em `../CLAUDE.md`, seção "Testes"). Rodam rápido e não exigem infraestrutura externa.
2. **Teste de contexto da aplicação (`SgcApiApplicationTests#contextLoads`)** — sobe o contexto Spring completo contra um MySQL real, validando que: (a) todas as migrações do Flyway aplicam sem erro; (b) o mapeamento das entidades JPA corresponde exatamente ao schema do banco (`ddl-auto: validate`). É o único teste que precisa de banco acessível.

**O que isso não cobre, por decisão explícita**: consultas JPQL novas (ex.: as dos relatórios de análise) não são exercitadas por teste automatizado — apenas o `contextLoads` garante que a sintaxe é válida; o retorno correto desses dados foi validado manualmente, com a aplicação rodando contra o banco real (ver seção 4). Essa é uma limitação conhecida e documentada, não um esquecimento.

## 2. Como rodar

```bash
# suíte completa (precisa de MySQL acessível — ver application.yml)
./mvnw test

# uma classe específica
./mvnw test -Dtest=PdfWriterTest

# um método específico
./mvnw test -Dtest=SgcApiApplicationTests#contextLoads
```

## 3. Cobertura atual

Estado em 2026-10-07: **143 testes**, todos passando (`BUILD SUCCESS`), organizados por módulo:

| Módulo | Classes de teste | O que é coberto |
|---|---|---|
| `common.util` | `CsvWriterTest`, `PdfWriterTest` | Geração de CSV (formato Excel pt-BR, neutralização de CSV injection) e de PDF (tabela, largura de coluna, abreviação de cabeçalho, páginas sem dados, orientação paisagem) — inclusive extração do texto real do PDF gerado (`PdfTextExtractor`) para validar o conteúdo renderizado, não só a assinatura do arquivo. |
| `domain.dashboard` | `DashboardServiceImplTest` | Cálculo dos indicadores do painel a partir de dados agregados. |
| `domain.notification` | `EmailNotificationSenderTest`, `NotificationServiceImplTest`, `ServiceOrderEmailTemplateTest`, `ServiceOrderNotificationListenerTest`, `WhatsAppNotificationSenderTest` | Envio por e-mail (`MockRestServiceServer`), orquestração WhatsApp → fallback e-mail, template HTML/texto por status, disparo do listener assíncrono sem duplicar nem notificar inativos. |
| `domain.report` | `AssetInsightsServiceImplTest`, `ReportServiceImplTest`, `ServiceOrderInsightsServiceImplTest`, `TraceabilityReportServiceImplTest` | Agregação dos relatórios (bens, OS, custo de manutenção, depreciação, bens críticos, movimentações, auditoria) e geração de CSV/PDF por relatório. |
| `domain.serviceorder` | `ServiceOrderServiceImplChannelTest`, `ServiceOrderServiceImplPeriodTest`, `ServiceOrderServiceImplResolveTest` | Canal de abertura (painel/WhatsApp), filtro por período, regras de resolução (exige laudo, bloqueia OS já resolvida/cancelada). |
| `domain.user` | `UserServiceImplFindOrCreateByPhoneTest` | Resolução/criação de usuário por telefone, incluindo o caso de colisão de nome (ver `../CLAUDE.md` e correção aplicada em `UserServiceImpl.resolveUniqueName`). |
| `domain.whatsapp` | `WhatsAppAutoReplyServiceTest`, `WhatsAppServiceOrderCommandTest`, `WhatsAppServiceOrderIntakeServiceTest`, `WhatsAppServiceOrderStatusQueryCommandTest`, `WhatsAppServiceOrderStatusQueryServiceTest`, `WhatsAppSignatureVerifierTest`, `WhatsAppWebhookControllerTest`, `WhatsAppWebhookPayloadTest` | Parsing de comando de abertura e de consulta de status, resolução de setor/bem, validação de assinatura HMAC do webhook (`X-Hub-Signature-256`), orquestração de resposta automática. |
| raiz (`br.com.fpo.sgc`) | `SgcApiApplicationTests` | Contexto completo + validação de schema (Flyway/`ddl-auto`). |

## 4. Testes manuais realizados (fora da suíte automatizada)

Documentados junto ao item que validam, para rastreabilidade:

- **WhatsApp real (RF14–RF17)**: webhook, envio, abertura de OS e consulta de status testados com mensagens reais via número de teste da Meta, incluindo um bug (`UnexpectedRollbackException`) só detectado nesse teste, não pelos mocks — ver `PLANO_IMPLEMENTACAO_MVP.md`, seção 7.4 (RF16).
- **Exportação em PDF**: PDF baixado e inspecionado visualmente para o relatório de Ordens de Serviço (paisagem, 12 colunas) e o de bens por categoria (retrato), incluindo o fix de quebra de cabeçalho (ver `PLANO_IMPLEMENTACAO_MVP.md`, seção 7.5).
- **Correção de bug de login**: colisão de nome de usuário entre cadastro manual e auto-criação via WhatsApp, reproduzida e corrigida contra o banco real.
- **Integração WhatsApp via ngrok**: passo a passo de configuração local documentado em `CONFIGURACAO_WHATSAPP_CLOUD_API.md`, seção 5.
- **Tratamento de exceções não previstas (2026-10-12)**: cada handler novo de `GlobalExceptionHandler` disparado contra a aplicação rodando, não só lido no código — CPF duplicado (antes 500 cru, agora 400 `business-error`), JSON malformado e enum inválido no corpo (400 `malformed-request`), nome de prédio duplicado sem pré-checagem no código (400 `data-integrity`) e usuário sem papel de admin acessando `/api/v1/dashboard` (403 `access-denied` com corpo, antes vazio). Dados de teste limpos (soft delete) depois da validação.
- **Auditoria completa nos cadastros mestres (2026-10-12)**: criado, editado e inativado um funcionário e um prédio de teste contra a aplicação rodando, confirmando as 3 entradas (`CREATE`/`UPDATE`/`DELETE`) em `/api/v1/audit-logs` com `performedBy` = usuário autenticado — ver `PLANO_IMPLEMENTACAO_MVP.md`, seção 8.

## 5. O que fica fora do escopo de teste deste repositório

- **Frontend**: a interface React (`frontend-sgc`) é um repositório separado e não é objeto dos testes deste projeto.
- **Testes de carga/performance**: não realizados — fora do escopo de um MVP acadêmico na escala de uma única instituição de ensino.
- **Testes end-to-end automatizados (navegador)**: verificações de UI foram feitas manualmente durante o desenvolvimento (ex.: botão de exportação de PDF, formato de data), sem suíte automatizada dedicada.
