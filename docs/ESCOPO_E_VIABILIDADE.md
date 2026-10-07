# Escopo e Viabilidade do MVP

> Documento exigido pelas diretrizes de TCC da FPO para a modalidade III — Proposta de Produto Tecnológico (MVP): "documentação que justifique a criação do produto, incluindo análise de viabilidade técnica e, quando aplicável, financeira" (`docs/apoio/Orientações de TCC para ADS -2026.pdf`). Complementa o contexto e os objetivos já descritos em `PLANO_IMPLEMENTACAO_MVP.md` (seções 1 e 2), sem repeti-los — aqui o foco é justificar a criação do produto e avaliar sua viabilidade.

## 1. Problema que origina o produto

A Faculdade Princesa do Oeste (FPO) não possui, hoje, um sistema centralizado de controle patrimonial nem de abertura/acompanhamento de manutenções. Solicitações de reparo são feitas de forma informal (papel, verbal, mensagens avulsas entre funcionários), o que causa atraso no atendimento, perda de informação sobre o que já foi pedido e nenhuma rastreabilidade histórica de manutenção por bem.

Uma pesquisa de campo conduzida junto a 13 respondentes (14 questões) confirmou alta aceitação do WhatsApp como canal preferido tanto para abrir um chamado quanto para receber notificação do andamento — por isso o produto não é "mais um sistema de chamados com um botão de WhatsApp a mais", e sim um sistema cujo canal de entrada e saída primário, ao lado do painel administrativo, é o WhatsApp.

## 2. Justificativa da criação do produto

- **Situação-problema real e vivenciada**: observação direta da rotina da instituição (requisito das diretrizes de TCC — tema originado de vivência na prática profissional), não um problema hipotético.
- **Retorno social concreto**: o produto resolvido é entregável e utilizável pela própria instituição que originou o problema (FPO), atendendo ao dever ético de retorno à comunidade mencionado nas diretrizes do curso.
- **Aplicação prática de conhecimentos do curso**: cobre back-end (API REST, persistência relacional, autenticação/autorização), integração com serviço de terceiro (WhatsApp Cloud API, e-mail transacional), modelagem de dados e front-end desacoplado — conhecimentos construídos ao longo da graduação em ADS.

## 3. Escopo do MVP

### 3.1 Dentro do escopo (ver critérios de aceite em `PLANO_IMPLEMENTACAO_MVP.md`, seção 11 — 100% atendidos)

1. Cadastro e controle de bens patrimoniais (tombamento, categoria, localização, responsável, estado de conservação, aquisição, foto).
2. Abertura de Ordem de Serviço (OS) pelo painel **ou** por mensagem de WhatsApp.
3. Atribuição de técnico, mudança de status e encerramento de OS com laudo.
4. Notificação automática do solicitante e do técnico a cada evento relevante da OS, por WhatsApp com fallback automático para e-mail.
5. Consulta de status de chamado diretamente pelo WhatsApp.
6. Histórico de manutenção por bem.
7. Painel de indicadores e relatórios agregados (categoria, período/técnico/status, custo × valor de aquisição, depreciação, bens críticos, movimentações, auditoria), exportáveis em CSV e PDF.
8. Autenticação JWT e autorização por papel (Administrador, Técnico, Solicitante).

### 3.2 Fora do escopo do MVP (deliberadamente adiado)

- Baixa/desfazimento patrimonial formal (motivo, data, responsável) — hoje coberta apenas pelo soft delete genérico do sistema.
- Health-check automatizado da integração de notificação (badge "WhatsApp: operacional/degradado" no painel).
- Massa de dados de demonstração (*seed*) além do usuário administrador de bootstrap.
- Máquina de estados de conversa (sessão) no chatbot do WhatsApp — decisão deliberada de manter cada mensagem interpretada isoladamente, para simplicidade de uso (ver `PLANO_IMPLEMENTACAO_MVP.md`, seção 6).

Essas exclusões estão registradas para que a banca entenda que são decisões de escopo conscientes, não lacunas não percebidas (ver também `DOCUMENTACAO_TECNICA_TCC.md`, seção 9, para o detalhamento técnico de cada limitação).

## 4. Viabilidade técnica

| Dimensão | Avaliação |
|---|---|
| **Maturidade da stack** | Java 21 (LTS) + Spring Boot 4.1, Spring Data JPA/Hibernate, MySQL, React 19/Vite — todas tecnologias amplamente adotadas no mercado, com documentação extensa e suporte de longo prazo, reduzindo risco de obsolescência durante o desenvolvimento do TCC. |
| **Equipe e tempo disponível** | Desenvolvimento individual (requisito do TCC), com apoio de ferramenta de codificação assistida por IA (Claude Code) para acelerar implementação e documentação, dentro da carga horária da disciplina de TCC (60h, quinto período). |
| **Dependência de serviço de terceiro (ponto de maior risco técnico)** | O diferencial do produto (WhatsApp) depende de uma plataforma externa (Meta). Esse risco foi identificado, avaliado e mitigado durante o próprio desenvolvimento: a integração inicialmente planejada (Evolution API, conexão não oficial) foi substituída pela **WhatsApp Cloud API oficial da Meta** após se confirmar uma onda de banimento de contas em APIs não oficiais em 2026 — decisão registrada e justificada em `PLANO_IMPLEMENTACAO_MVP.md` (seção 0.2) e **validada na prática** com mensagens reais (RF14–RF17, concluídos). |
| **Resiliência a falha do canal principal** | Implementado fallback automático para e-mail (SendGrid) quando o WhatsApp falhar (token expirado, número restrito, fora da janela de atendimento de 24h, etc.), com retentativa e registro de cada tentativa — ver `PLANO_IMPLEMENTACAO_MVP.md`, seção 9. Mitiga o risco de o produto inteiro depender de um único canal gerenciado por terceiro. |
| **Compatibilidade com o schema do banco** | Evolução do schema controlada por migrações Flyway versionadas e sequenciais (`V1`–`V19`), com `ddl-auto: validate` — qualquer divergência entre entidade JPA e schema real é detectada na inicialização da aplicação, não em produção silenciosamente. |

**Conclusão de viabilidade técnica**: o produto é tecnicamente viável com as ferramentas e o tempo disponíveis para um TCC, e o principal risco técnico identificado (dependência da Meta) já foi mitigado e validado em execução real antes da entrega.

## 5. Viabilidade financeira

O MVP foi deliberadamente desenhado para ter **custo próximo de zero** durante o desenvolvimento e a demonstração em banca, usando exclusivamente camadas gratuitas dos serviços externos envolvidos:

| Item | Custo no MVP | Observação |
|---|---|---|
| Hospedagem do banco de dados (MySQL) | R$ 0 | Instância local, sem custo de nuvem durante o desenvolvimento/demonstração. |
| WhatsApp Cloud API (Meta) | R$ 0 | Número de teste da própria Meta (até 5 destinos verificados) ou Twilio Sandbox — sem processo de aprovação, sem custo. Mensagens de resposta dentro da janela de 24h (o caso de uso principal: usuário escreve → bot responde) são gratuitas; só mensagem iniciada pelo sistema fora dessa janela usaria *template* pago, cenário irrelevante no volume de uma demonstração acadêmica. |
| E-mail transacional (SendGrid, fallback) | R$ 0 | Dentro do tier gratuito do SendGrid (100 e-mails/dia), suficiente para o volume de notificações do MVP. |
| Backend/frontend | R$ 0 | Código aberto (Spring Boot, React), sem licenciamento. |
| Domínio/túnel público para o webhook (desenvolvimento) | R$ 0 | ngrok (camada gratuita) usado para expor o webhook localmente durante testes — ver `CONFIGURACAO_WHATSAPP_CLOUD_API.md`, seção 5. |

**Se a FPO decidir adotar o sistema em produção real** (fora do escopo deste TCC), os custos recorrentes estimados seriam:

| Item | Estimativa | Observação |
|---|---|---|
| VPS de baixo custo (backend + MySQL) | ~R$ 20–50/mês | Suficiente para a escala de uma única instituição de ensino; não há requisito de alta disponibilidade. |
| Domínio próprio + certificado HTTPS | ~R$ 40–60/ano | O webhook da Meta exige endpoint HTTPS público — pré-requisito de produção, não do MVP acadêmico. |
| WhatsApp Cloud API em produção | Variável, baixo | Cobrança por mensagem de *template* entregue fora da janela de 24h; o uso previsto (resposta a mensagem recebida) continua gratuito. Verificação de número de negócio junto à Meta é gratuita. |
| SendGrid, se o volume ultrapassar o tier gratuito | A partir de ~US$ 20/mês | Só necessário se o volume de notificações superar 100/dia — improvável na escala de uma instituição de ensino. |

**Conclusão de viabilidade financeira**: o MVP não exige nenhum investimento financeiro para ser desenvolvido, testado e demonstrado em banca. Uma eventual adoção em produção pela FPO teria custo mensal baixo (dezenas de reais), compatível com o porte da instituição.

## 6. Critérios de aceite (resultado)

Os 5 critérios de aceite definidos para o MVP estão **100% atendidos** — ver detalhamento e evidências em `PLANO_IMPLEMENTACAO_MVP.md`, seção 11.
