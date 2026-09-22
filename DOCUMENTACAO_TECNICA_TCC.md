# Documentação Técnica da Implementação — Insumo para Revisão do TCC

> Este documento tem finalidade exclusivamente redacional: descrever, em linguagem acadêmica, o que foi efetivamente implementado no backend do Sistema de Gerenciamento e Controle Patrimonial Integrado ao WhatsApp, de modo a subsidiar a atualização do texto do TCC (capítulos de Metodologia, Desenvolvimento/Implementação e Resultados). Não substitui o TCC nem propõe nova estrutura de capítulos — apenas consolida, com precisão técnica, o estado real do artefato de software produzido até o momento (2026-09-22), para que as seções correspondentes do trabalho acadêmico sejam ajustadas para refletir o que foi construído. Espelha o levantamento de código consolidado em `PLANO_IMPLEMENTACAO_MVP.md`.

## 1. Escopo do que foi implementado

O desenvolvimento cobre hoje o ciclo administrativo completo do sistema: cadastro e controle de bens patrimoniais (com categoria, responsável, dados de aquisição, estado de conservação e foto), gestão de ordens de serviço do abertura ao encerramento pelo painel administrativo, restrição de acesso por papel de usuário, notificação automática do solicitante e do técnico por e-mail a cada mudança relevante da ordem de serviço (com registro de tentativas e retentativa com backoff), e um conjunto de relatórios agregados (bens, ordens de serviço, custo de manutenção, depreciação patrimonial, tempo por etapa e auditoria), todos exportáveis em CSV. Esse conjunto corresponde à camada de backend (API REST) descrita na arquitetura do trabalho.

A integração com o WhatsApp (recebimento de mensagens, chatbot de abertura/consulta de chamado e envio de notificação pelo próprio WhatsApp), prevista no TCC como diferencial central do produto, ainda não foi implementada; consta neste documento como escopo pendente (seção 7). Uma decisão de engenharia está em avaliação prática quanto ao provedor dessa integração — ver observação ao final da seção 7 — mas isso não altera o fato de que, no código, nenhum componente de WhatsApp existe ainda.

## 2. Tecnologias e ferramentas utilizadas

A escolha das tecnologias seguiu o que foi definido no referencial metodológico do TCC (arquitetura MVC, backend e frontend desacoplados, comunicação via API REST/JSON, banco de dados relacional). O Quadro 1 relaciona as tecnologias efetivamente empregadas na implementação do backend, com as respectivas versões utilizadas no projeto.

**Quadro 1 — Stack tecnológica do backend**

| Camada / Finalidade | Tecnologia | Versão |
|---|---|---|
| Linguagem de programação | Java | 21 (LTS) |
| Framework de aplicação | Spring Boot | 4.1.0 |
| Persistência e mapeamento objeto-relacional | Spring Data JPA (Hibernate) | conforme Spring Boot 4.1.0 |
| Banco de dados relacional | MySQL | driver `mysql-connector-j` |
| Controle de versionamento de esquema de banco de dados | Flyway (`flyway-mysql`) | conforme Spring Boot 4.1.0 |
| Autenticação e autorização | Spring Security + JSON Web Token (JWT) | `spring-boot-starter-security`; biblioteca `com.auth0:java-jwt` 4.4.0 |
| Autorização por papel a nível de endpoint | Spring Security Method Security (`@PreAuthorize`) | `@EnableMethodSecurity` |
| Hash de senhas | BCrypt (`BCryptPasswordEncoder`) | — |
| Validação de dados de entrada | Bean Validation (Jakarta Validation) | `spring-boot-starter-validation` |
| Envio de notificação por e-mail | SendGrid, via `RestClient` do próprio Spring (sem dependência nova) | API `POST /v3/mail/send` |
| Documentação de API | springdoc-openapi (Swagger UI) | 2.8.5 |
| Redução de código repetitivo (getters/setters/builders) | Lombok | — |
| Gerenciador de build e dependências | Apache Maven (via wrapper `mvnw`) | — |
| Ferramenta de codificação assistida por IA | Claude Code (Anthropic) | — |

A persistência de dados foi implementada sobre um banco de dados relacional (MySQL), com o mapeamento objeto-relacional realizado pela especificação Jakarta Persistence (JPA), cuja implementação de referência empregada pelo Spring Boot é o Hibernate. A evolução do esquema do banco de dados é controlada por migrações versionadas com a ferramenta Flyway, seguindo o padrão `V<n>__<descrição>.sql`; a estratégia de geração de esquema do Hibernate foi configurada como `validate`, de modo que o mapeamento das entidades deve necessariamente corresponder ao esquema já aplicado pelas migrações, e não o contrário — ou seja, toda alteração estrutural no modelo de dados exige uma nova migração, nunca a geração automática do esquema pelo framework. Até o presente momento foram aplicadas dezenove migrações (V1 a V19), abrangendo usuários, localizações (empresas, prédios e setores), bens patrimoniais (incluindo categoria, responsável, dados de aquisição, estado de conservação e foto), ordens de serviço (incluindo canal de abertura e custo), log de auditoria, marcas, funcionários, materiais, produtos, movimentações de bens, histórico estruturado de status da ordem de serviço e notificações.

A autenticação foi implementada de forma *stateless*, por meio de JSON Web Tokens assinados com o algoritmo HS256, com expiração de oito horas e identificação do emissor (*issuer*) do token. O token carrega, como *claim* adicional, o papel (*role*) do usuário autenticado. As senhas dos usuários são armazenadas com hash unidirecional (BCrypt), nunca em texto plano. A autorização por papel é aplicada a nível de cada endpoint de escrita por meio de anotações declarativas (`@PreAuthorize`), segundo uma política única para todo o sistema (ver seção 4).

A documentação da API é gerada automaticamente a partir das anotações do código-fonte (springdoc-openapi), com interface interativa exposta publicamente (sem autenticação) na rota `/swagger-ui.html`, o que permite a exploração dos endpoints disponíveis diretamente no navegador — recurso útil tanto para o desenvolvimento quanto para a demonstração do sistema em banca.

## 3. Convenções de desenvolvimento adotadas

Como boa prática de engenharia de software, adotou-se a convenção de que **todo o código-fonte — nomes de classes, atributos, métodos, pacotes e variáveis — é escrito em língua inglesa**, seguindo o padrão amplamente utilizado pela comunidade de desenvolvimento Java/Spring. Já **os elementos voltados ao usuário final ou à leitura por um avaliador humano do domínio de negócio — mensagens de erro, mensagens de validação, textos de notificação e comentários que documentam regras de negócio específicas — são escritos em português (pt-BR)**, por se tratar de um sistema destinado a um público brasileiro. Essa separação (código técnico em inglês, comunicação de domínio em português) é uma prática corrente no mercado de desenvolvimento de software e foi mantida deliberadamente ao longo de toda a implementação, inclusive nas etapas ainda por realizar.

## 4. Arquitetura do sistema

O backend segue o padrão arquitetural MVC (*Model-View-Controller*), adaptado à realidade de uma API REST — isto é, sem camada de visualização (*view*) no sentido tradicional, uma vez que a camada de apresentação é delegada a um cliente externo (frontend desacoplado), conforme previsto no desenho original do TCC.

A organização interna do código segue uma estrutura por **domínio de negócio**, e não por camada técnica: cada entidade de negócio relevante (bem patrimonial e sua categoria, ordem de serviço e seu histórico de status, usuário, empresa, prédio, setor, funcionário, marca, material, produto, log de auditoria, notificação, entre outras) constitui um módulo autocontido, composto pelos seguintes elementos:

- **Entidade** — classe anotada com `@Entity`, mapeada para uma tabela do banco de dados via JPA;
- **Repositório** — interface do Spring Data JPA responsável pelo acesso a dados, com métodos de consulta nomeados por convenção;
- **Serviço** (interface e implementação) — camada onde residem as regras de negócio, o lançamento de exceções de domínio e o registro de auditoria;
- **Controlador** — classe REST enxuta, responsável apenas por expor os endpoints HTTP, declarar a política de autorização por papel e delegar a chamada ao serviço correspondente, sem lógica de negócio;
- **Objetos de transferência de dados (DTO)** — objetos imutáveis (*records* da linguagem Java) que representam o contrato de entrada e saída da API, desacoplados da estrutura interna das entidades persistidas;
- **Mapeador (*mapper*)** — componente responsável pela conversão entre entidade e DTO, escrito manualmente (sem uso de bibliotecas de mapeamento automático).

Dois módulos fogem dessa estrutura por não representarem um CRUD de entidade própria: o **painel de indicadores** (*dashboard*) e os **relatórios**, que apenas agregam dados de outros módulos; e o módulo de **notificação**, descrito a seguir, que é orientado a eventos em vez de a requisições diretas.

Essa organização por domínio, replicada de forma consistente em todos os módulos implementados, caracteriza uma arquitetura em camadas horizontais dentro de cada módulo vertical, favorecendo a manutenibilidade e a previsibilidade do código à medida que novos módulos de domínio são adicionados.

Como convenções transversais a todos os módulos, destacam-se:

- **Exclusão lógica (*soft delete*)**: nenhum registro é fisicamente removido do banco de dados nas operações comuns de exclusão; em vez disso, um indicador booleano de "ativo" é definido como falso, e as consultas de listagem passam a considerar apenas os registros ativos. Esse mecanismo preserva o histórico de dados para fins de auditoria e rastreabilidade — requisito relevante em um sistema de controle patrimonial.
- **Auditoria de operações**: toda operação de criação, atualização ou exclusão lógica realizada por meio da API gera um registro de auditoria (entidade, identificador do registro, ação realizada, autor da ação e detalhes textuais), persistido na mesma transação da alteração de dados. O autor registrado é sempre o usuário efetivamente autenticado na requisição, nunca um valor informado livremente pelo cliente da API.
- **Autorização por papel**: cada operação de escrita em um cadastro mestre (usuários, empresas, prédios, setores, funcionários, marcas, materiais, produtos, categorias de bem, bens patrimoniais, movimentações) exige o papel de administrador; leituras são abertas a qualquer usuário autenticado. Para ordens de serviço, a abertura é permitida a qualquer usuário autenticado, a atribuição de técnico e a exclusão exigem administrador, e a resolução/mudança de status exige administrador ou técnico. O painel de indicadores e os relatórios são restritos a administradores.
- **Paginação**: os endpoints de listagem retornam resultados paginados, com tamanho de página e critério de ordenação configuráveis por parâmetro de requisição.
- **Tratamento centralizado de exceções**: um manipulador global de exceções traduz exceções de domínio (recurso não encontrado, violação de regra de negócio, falha de validação de entrada, parâmetro de requisição com tipo inválido) em respostas HTTP padronizadas, no formato RFC 7807 (*Problem Details for HTTP APIs*), o que garante uniformidade na comunicação de erros da API independentemente do módulo que os originou.
- **Notificação orientada a eventos**: cada mudança de status relevante de uma ordem de serviço publica um evento de domínio interno à aplicação, processado de forma assíncrona somente após a confirmação (*commit*) da transação que originou a mudança — de modo que uma eventual falha no envio da notificação nunca reverte nem afeta a operação de negócio que a originou.
- **Exportação de relatórios em CSV**: os relatórios agregados podem ser exportados em formato CSV compatível com Excel em português (separador `;`, codificação UTF-8 com marca de ordem de byte), com neutralização de conteúdo que poderia ser interpretado como fórmula por planilhas eletrônicas (*CSV injection*).

## 5. Modelo de dados implementado

O modelo de dados implementado cobre, com nomenclatura em língua inglesa nas classes (mantendo a correspondência conceitual com os termos de domínio em português definidos no referencial teórico do TCC — tombamento, carga patrimonial, ordem de serviço), as seguintes entidades:

- **Usuário** (`User`): credenciais de acesso ao sistema, com papel de acesso (administrador, técnico de manutenção ou solicitante), utilizado para autenticação via JWT e para autorização por papel a nível de endpoint.
- **Empresa** (`Company`): unidade organizacional de topo, permitindo que o sistema opere em regime multiempresa (multi-tenant), com razão social, CNPJ, contato e identidade visual (logotipo).
- **Prédio** (`Building`) e **Setor** (`Sector`): hierarquia de localização física dos bens patrimoniais, vinculada a uma empresa; o setor pode ter um funcionário responsável designado como gestor.
- **Funcionário** (`Employee`): cadastro de colaboradores da instituição (nome, CPF, matrícula, cargo, empresa e setor de lotação), utilizado como referência organizacional independente do usuário de acesso ao sistema e, desde a introdução da carga patrimonial, como responsável formal por um bem.
- **Categoria de bem** (`AssetCategory`): taxonomia formal do bem patrimonial (ex.: Informática, Mobiliário, Elétrico, Predial), vinculada opcionalmente a cada bem.
- **Bem patrimonial** (`Asset`): entidade central do controle patrimonial, identificada por um código de tombamento único, com descrição, modelo, número de série, situação (disponível, em uso, em manutenção ou baixado), setor onde se encontra, categoria, funcionário responsável (carga patrimonial), data e valor de aquisição, estado de conservação e fotografia.
- **Movimentação de bem** (`AssetMovement`): registro histórico de transferência de um bem entre setores, com motivo e data da movimentação — funcionalidade complementar ao controle patrimonial, não prevista originalmente no modelo de dados do TCC, mas coerente com o objetivo de rastreabilidade do acervo.
- **Marca** (`Brand`), **Material** (`Material`) e **Produto** (`Product`): catálogo auxiliar de classificação de itens (marca e categoria de material, combinados em um produto), implementado como módulo independente, não integrado como taxonomia do bem patrimonial (que usa `AssetCategory`, dedicada a esse fim).
- **Ordem de serviço** (`ServiceOrder`): registro formal de uma solicitação de manutenção, com título, descrição do problema, prioridade (baixa, média, alta ou urgente), situação (aberta, em andamento, resolvida ou cancelada), canal de abertura (painel ou WhatsApp — hoje sempre painel, na ausência do canal WhatsApp), solicitante, técnico responsável (quando atribuído), bem patrimonial vinculado (quando aplicável), setor de origem, laudo/solução técnica, custo do reparo (quando conhecido) e data de encerramento.
- **Histórico de status da ordem de serviço** (`ServiceOrderStatusLog`): registro estruturado de cada mudança de situação de uma ordem de serviço (situação anterior, nova situação, autor da mudança, observação), alimentado automaticamente a cada transição de status.
- **Notificação** (`Notification`): registro de cada tentativa de envio de notificação relativa a uma ordem de serviço (destinatário, canal, resultado do envio, número da tentativa, erro quando houver), servindo como trilha de auditoria do próprio mecanismo de notificação.
- **Log de auditoria** (`AuditLog`): registro cronológico e imutável das operações de criação, atualização e exclusão lógica realizadas em qualquer módulo do sistema, com autor, ação e detalhamento textual.

A evolução deste modelo de dados em relação ao desenho conceitual inicial do TCC — por exemplo, a introdução de uma hierarquia empresa/prédio/setor em vez de uma entidade única de localização, ou a criação dos módulos de funcionários e de categoria de bem — decorre de decisões de implementação tomadas durante o desenvolvimento e deve ser refletida no capítulo de modelagem de dados do TCC, preferencialmente por meio da atualização do diagrama entidade-relacionamento originalmente proposto.

## 6. Funcionalidades implementadas

Em correspondência com os objetivos específicos do TCC, encontram-se implementadas e funcionais, via API REST, as seguintes funcionalidades:

1. Autenticação de usuários por nome de usuário e senha, com emissão de token JWT, e autorização por papel de acesso (administrador, técnico, solicitante) aplicada individualmente a cada endpoint de escrita e de leitura restrita da API.
2. Cadastro, edição e inativação de usuários do sistema.
3. Cadastro, edição e inativação de empresas, prédios e setores, compondo a estrutura de localização física do patrimônio.
4. Cadastro, edição e inativação de categorias de bem patrimonial.
5. Cadastro, edição, inativação e consulta paginada de bens patrimoniais, com identificação por tombamento único, controle de situação, categoria, responsável (carga patrimonial), data e valor de aquisição, estado de conservação e fotografia (upload dedicado).
6. Consulta de bens patrimoniais por setor, por código de tombamento, por situação, por categoria e por responsável.
7. Registro de movimentação (transferência) de bens entre setores, com motivo e data.
8. Abertura de ordens de serviço pelo painel administrativo, associadas ou não a um bem patrimonial específico, com definição de prioridade e descrição do problema; quando associada a um bem, o sistema atualiza automaticamente a situação do bem para "em manutenção".
9. Atribuição de uma ordem de serviço a um técnico de manutenção, com atualização automática da situação da ordem para "em andamento".
10. Mudança de situação de uma ordem de serviço (com observação), incluindo cancelamento, com validações que impedem transições inválidas (ex.: nova mudança ou resolução em ordem já resolvida ou cancelada) e registro estruturado de cada transição no histórico de status.
11. Encerramento de uma ordem de serviço mediante registro do laudo/solução técnica e, opcionalmente, do custo do reparo, com retorno automático do bem associado à situação "em uso".
12. Consulta de ordens de serviço por situação, por solicitante, por técnico, por bem patrimonial (histórico de manutenção do bem) e por período (data de abertura ou de encerramento), além de listagem paginada geral e consulta do histórico estruturado de status de cada ordem.
13. Notificação automática, por e-mail, do solicitante, do técnico atribuído e de quem resolveu a ordem de serviço, a cada evento relevante do ciclo de vida da ordem (abertura, atribuição, mudança de situação, resolução), com retentativa e registro de cada tentativa de envio para fins de auditoria; falha no envio nunca afeta a operação da ordem de serviço.
14. Painel de indicadores (*dashboard*) com total de bens patrimoniais, distribuição por empresa e por situação, quantidade de chamados ativos, custo de manutenção acumulado e quantidade de bens sob controle de depreciação — todos calculados a partir de dados reais, sem valores fixos de demonstração.
15. Relatório de bens patrimoniais por categoria e situação, e relatório de ordens de serviço por período/técnico/situação (com tempo médio de atendimento e os bens com mais manutenções), ambos exportáveis em CSV.
16. Relatório de custo de manutenção comparado ao valor de aquisição do bem, sinalizando candidatos a substituição, exportável em CSV.
17. Relatórios de apoio à análise acadêmica do TCC: evolução mensal de ordens de serviço, tempo médio por etapa do fluxo de atendimento, patrimônio com depreciação linear por categoria/setor, bens críticos, resumo de movimentações e resumo de auditoria — todos exportáveis em CSV.
18. Registro automático de auditoria para todas as operações de escrita relevantes descritas acima.
19. Cadastro auxiliar de marcas, materiais e produtos, e de funcionários da instituição.
20. Documentação interativa e navegável de toda a API por meio do Swagger UI.

## 7. Limitações identificadas e escopo pendente

Para fins de precisão acadêmica, é relevante que o texto do TCC reconheça explicitamente as seguintes limitações do estado atual da implementação, de modo a não superestimar o que foi entregue frente ao que foi originalmente proposto:

- **Baixa/desfazimento formal**: a retirada de um bem do acervo continua tratada apenas pelo mecanismo geral de exclusão lógica do sistema, sem registro estruturado de motivo, data e responsável pela baixa.
- **Canal de abertura da ordem de serviço**: o modelo de dados já registra o canal de abertura (painel ou WhatsApp), mas, na ausência da integração com o WhatsApp, toda ordem de serviço é aberta hoje pelo canal painel.
- **Integração com WhatsApp**: não implementada. Não existe, até o momento, recebimento de mensagens via webhook, máquina de estados de conversa (chatbot) para abertura e consulta de chamados, nem qualquer envio de notificação pelo canal WhatsApp — o único canal de notificação em funcionamento hoje é o e-mail. As variáveis de configuração relativas a essa integração já constam do arquivo de configuração da aplicação, como preparação para a etapa subsequente do desenvolvimento, mas ainda não estão conectadas a nenhum componente funcional.
- **Health-check da integração de notificação**: não implementado um endpoint que verifique periodicamente a saúde do canal de notificação externo e a exponha no painel administrativo.
- **Exportação de relatórios em PDF**: os relatórios são exportáveis em JSON (resposta padrão da API) e CSV; a exportação em PDF, mencionada como possibilidade, não foi implementada.
- **Frontend**: este repositório contém exclusivamente o backend (API REST); a interface de usuário em React, prevista na arquitetura do TCC, é desenvolvida separadamente e não é objeto desta documentação.
- **Testes automatizados e dados de demonstração**: ainda não foi implementada uma suíte de testes automatizados cobrindo os fluxos críticos do sistema além do teste padrão de carregamento do contexto da aplicação, tampouco uma massa de dados de demonstração (*seed*) para apresentação em banca — existe apenas um usuário administrador de inicialização, necessário para o primeiro acesso ao sistema após a restrição de acesso por papel entrar em vigor.

**Observação sobre o provedor da integração WhatsApp.** Uma decisão de engenharia está em avaliação prática desde 2026-09-21: substituir a Evolution API (conexão não oficial, sujeita a banimento pela Meta) pela WhatsApp Cloud API oficial como integração primária, testada via número de teste da Meta ou Twilio Sandbox. Essa decisão está registrada e fundamentada em `PLANO_IMPLEMENTACAO_MVP.md` (seção 0.2), mas **ainda não foi implementada nem validada em execução**. Enquanto isso não ocorrer, o texto oficial do TCC deve continuar descrevendo a Evolution API como a integração planejada, conforme já orientado naquele documento; a atualização do texto acadêmico para refletir a Cloud API só deve ser feita depois que a integração estiver de fato implementada e validada na prática.

## 8. Síntese para o texto do TCC

Em síntese, o que pode ser afirmado, com precisão técnica, sobre o estado atual do desenvolvimento é que **o núcleo administrativo do sistema de controle patrimonial e de ordens de serviço, operado por meio de uma API REST desenvolvida em Java/Spring Boot com persistência em MySQL, encontra-se implementado e funcional em profundidade**, cobrindo o ciclo completo de cadastro de bens (com categoria, carga patrimonial, dados de aquisição e estado de conservação), abertura, atribuição, mudança de situação e encerramento de ordens de serviço pelo painel administrativo, com autenticação via JWT, autorização por papel a nível de endpoint, exclusão lógica, trilha de auditoria em todas as operações relevantes, notificação automática por e-mail dos envolvidos em cada ordem de serviço, e um conjunto amplo de relatórios agregados exportáveis em CSV, incluindo indicadores de dashboard calculados exclusivamente a partir de dados reais. **A integração com o WhatsApp (chatbot para abertura/consulta de chamados e notificação pelo canal WhatsApp), que constitui o diferencial central proposto pelo TCC, permanece como etapa subsequente do desenvolvimento**, ainda não iniciada, devendo o texto acadêmico apresentá-la como trabalho em andamento ou como trabalho futuro, conforme o estágio em que o TCC se encontrar no momento da revisão.
