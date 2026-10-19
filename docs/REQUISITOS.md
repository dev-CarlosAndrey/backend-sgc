# Requisitos do Sistema

Este documento espelha a seção 6.5.4 (Requisitos do Sistema) do texto oficial do TCC. Os identificadores, a redação e a numeração dos quadros são os mesmos do TCC: ao alterar um requisito, atualize os dois lugares. O status de implementação de cada item e o histórico das decisões ficam em [`PLANO_IMPLEMENTACAO_MVP.md`](PLANO_IMPLEMENTACAO_MVP.md).

## Origem e convenções

Os requisitos foram definidos a partir de três fontes: a observação da rotina de manutenção da instituição, na qual as solicitações eram feitas em papel, de forma verbal ou por mensagens avulsas; o questionário aplicado aos colaboradores (14 questões, 13 respondentes), que apontou o WhatsApp como o canal preferido para abrir chamados e acompanhar o atendimento; e os conceitos de gestão patrimonial do referencial teórico, como tombamento, carga patrimonial e ordem de serviço.

Seguindo a classificação de Sommerville (2007):

- **RF** (requisito funcional): serviço que o sistema deve oferecer.
- **RNF** (requisito não funcional): qualidade ou restrição sob a qual os serviços operam.
- **RN** (regra de negócio): comportamento esperado em situações que a descrição de um requisito não cobre sozinha.

Prioridade dos requisitos funcionais: **essencial** (sem ele o sistema não atende ao problema proposto), **importante** (o sistema funciona sem ele, mas com perda relevante de valor) e **desejável** (complementa o produto sem comprometer o ciclo principal).

## Quadro 1: Atores do sistema

O perfil de cada usuário é gravado no cadastro e enviado no token de acesso; cada operação da API verifica se o perfil tem permissão para executá-la. O solicitante que escreve pelo WhatsApp não precisa de cadastro prévio, pois o sistema o cria a partir do número de telefone.

| Ator | Quem é na instituição | O que faz no sistema |
|---|---|---|
| Administrador | Setor administrativo ou de patrimônio | Mantém todos os cadastros, atribui técnicos às ordens de serviço, consulta o painel de indicadores e emite relatórios |
| Técnico de manutenção | Equipe responsável pelos reparos | Recebe as ordens de serviço atribuídas, altera a situação e registra o laudo e o custo do reparo |
| Solicitante | Professor ou funcionário | Abre ordens de serviço pelo painel ou pelo WhatsApp e acompanha a situação dos próprios chamados |
| WhatsApp Cloud API (Meta) | Sistema externo | Entrega ao sistema as mensagens recebidas no número da instituição e envia as respostas e notificações |
| SendGrid | Sistema externo | Envia as notificações por e-mail quando o WhatsApp não consegue entregar |

## Quadro 2: Requisitos funcionais

34 requisitos em oito módulos, todos implementados e validados com a aplicação em execução, inclusive os da integração com o WhatsApp, testados com mensagens reais pelo número de teste da Meta.

| ID | Requisito | Ator | Prioridade |
|---|---|---|---|
| **Acesso e usuários** | | | |
| RF01 | Autenticar o usuário por nome de usuário e senha, emitindo um token de acesso válido por 8 horas | Todos | Essencial |
| RF02 | Restringir cada operação ao perfil autorizado (administrador, técnico ou solicitante) | Sistema | Essencial |
| RF03 | Cadastrar, editar, consultar e inativar usuários, com nome, e-mail, telefone e perfil | Administrador | Essencial |
| **Estrutura organizacional** | | | |
| RF04 | Manter a hierarquia de localização: empresas (razão social, CNPJ e logotipo), prédios (código e endereço) e setores, cada setor com um funcionário gestor opcional | Administrador | Essencial |
| RF05 | Cadastrar, editar e inativar funcionários, com CPF, matrícula, cargo, empresa e setor de lotação | Administrador | Importante |
| **Controle patrimonial** | | | |
| RF06 | Manter as categorias de bens, como informática, mobiliário e climatização | Administrador | Importante |
| RF07 | Cadastrar, editar e inativar bens patrimoniais com número de tombamento único, nome, modelo, número de série, setor, categoria, situação, funcionário responsável (carga patrimonial), data e valor de aquisição e estado de conservação | Administrador | Essencial |
| RF08 | Anexar uma fotografia ao bem, nos formatos PNG, JPEG ou WebP, com até 5 MB | Administrador | Desejável |
| RF09 | Consultar bens com busca por texto e filtros por setor, tombamento, situação, categoria e responsável, com paginação | Todos | Essencial |
| RF10 | Registrar a movimentação de um bem entre setores, com setor de origem, setor de destino, motivo e data | Administrador | Importante |
| RF11 | Manter o catálogo auxiliar de marcas, materiais e produtos | Administrador | Desejável |
| **Ordens de serviço** | | | |
| RF12 | Abrir uma ordem de serviço pelo painel, com título, descrição do problema, prioridade (baixa, média, alta ou urgente), setor e, opcionalmente, o bem afetado | Solicitante e administrador | Essencial |
| RF13 | Atribuir um técnico de manutenção à ordem de serviço | Administrador | Essencial |
| RF14 | Alterar a situação da ordem de serviço para em andamento ou cancelada, com observação | Técnico e administrador | Essencial |
| RF15 | Encerrar a ordem de serviço registrando o laudo técnico e, opcionalmente, o custo do reparo | Técnico e administrador | Essencial |
| RF16 | Consultar ordens de serviço com busca por texto e filtros por situação, solicitante, técnico, bem e período de abertura ou de encerramento | Todos | Essencial |
| RF17 | Exibir o histórico de manutenção de um bem, com todas as ordens de serviço vinculadas a ele | Todos | Importante |
| RF18 | Registrar e exibir o histórico de situações de cada ordem de serviço, com situação anterior, nova situação, autor, observação e data | Sistema | Importante |
| **Integração com o WhatsApp** | | | |
| RF19 | Receber as mensagens enviadas ao número da instituição por meio do webhook da WhatsApp Cloud API | WhatsApp Cloud API | Essencial |
| RF20 | Abrir uma ordem de serviço a partir de uma mensagem no formato Setor, Bem (opcional) e Problema, respondendo com o número da ordem criada | Solicitante | Essencial |
| RF21 | Informar pelo WhatsApp a situação dos chamados do próprio número: os cinco mais recentes com o comando Status, ou um chamado específico com Status seguido do número | Solicitante | Essencial |
| RF22 | Responder com as instruções de uso quando a mensagem não corresponder a nenhum comando, e com a explicação do erro quando o setor ou o bem informado não existir | Sistema | Importante |
| **Notificações** | | | |
| RF23 | Notificar automaticamente o solicitante e o técnico atribuído na abertura, na atribuição de técnico, em cada mudança de situação e no encerramento da ordem de serviço, usando o WhatsApp como canal principal | Sistema | Essencial |
| RF24 | Enviar a notificação por e-mail quando o WhatsApp falhar após as retentativas, com o número da ordem, a situação e uma barra de progresso do atendimento | Sistema e SendGrid | Importante |
| RF25 | Registrar cada tentativa de envio, com canal, resultado, número da tentativa e mensagem de erro | Sistema | Importante |
| **Indicadores e relatórios** | | | |
| RF26 | Exibir um painel com total de bens, bens por empresa e por situação, chamados ativos, custo acumulado de manutenção e bens sob controle de depreciação | Administrador | Essencial |
| RF27 | Emitir relatório de bens por categoria e situação | Administrador | Importante |
| RF28 | Emitir relatório de ordens de serviço por período, técnico e situação, com tempo médio de atendimento e os bens com mais manutenções | Administrador | Essencial |
| RF29 | Emitir relatório do custo de manutenção comparado ao valor de aquisição, indicando os bens candidatos a substituição acima de um percentual definido, com padrão de 50% | Administrador | Importante |
| RF30 | Emitir relatórios de análise: evolução mensal das ordens de serviço, tempo médio por etapa do atendimento, patrimônio com depreciação linear e bens críticos | Administrador | Desejável |
| RF31 | Emitir relatórios de canal: ordens de serviço abertas pelo painel e pelo WhatsApp, e taxa de entrega das notificações por canal | Administrador | Importante |
| RF32 | Exportar os relatórios em CSV e em PDF | Administrador | Importante |
| **Auditoria e rastreabilidade** | | | |
| RF33 | Registrar em log de auditoria toda inclusão, alteração e inativação, com entidade, ação, autor, detalhes e data | Sistema | Essencial |
| RF34 | Emitir relatórios de movimentações de bens e de auditoria, filtráveis por período, entidade e usuário | Administrador | Importante |

## Quadro 3: Requisitos não funcionais

22 requisitos por categoria de qualidade. A última coluna indica como cada um é atendido, o que permite verificá-lo no código ou na aplicação em execução.

| ID | Requisito | Como é atendido |
|---|---|---|
| **Segurança** | | |
| RNF01 | As senhas não podem ser armazenadas em texto puro | Hash BCrypt antes de gravar no banco de dados |
| RNF02 | O acesso à API deve exigir autenticação sem manter sessão no servidor | Token JWT assinado com HS256, válido por 8 horas e enviado no cabeçalho Authorization; apenas o login, a documentação da API e o webhook são públicos |
| RNF03 | O webhook do WhatsApp só pode aceitar mensagens que venham de fato da Meta | Validação da assinatura X-Hub-Signature-256 (HMAC-SHA256 com o segredo do aplicativo, comparada em tempo constante) e do token de verificação no cadastro do webhook |
| RNF04 | Chaves e segredos não podem constar no código-fonte versionado | Chave do SendGrid, token do WhatsApp, segredo do aplicativo e segredo do JWT lidos apenas de variáveis de ambiente |
| RNF05 | As respostas de erro não podem expor detalhes internos | Tratamento centralizado de exceções; um erro inesperado devolve mensagem genérica e o detalhe fica apenas no log; a falha de login usa a mesma mensagem para usuário inexistente e senha incorreta |
| RNF06 | Os arquivos exportados não podem executar fórmulas ao serem abertos em planilhas | Células do CSV que começam com =, +, - ou @ recebem um apóstrofo no início |
| **Confiabilidade** | | |
| RNF07 | Uma falha no envio de notificação não pode desfazer nem travar a operação da ordem de serviço | A notificação é disparada por evento, de forma assíncrona, somente depois que a transação da ordem de serviço é confirmada no banco |
| RNF08 | O sistema não pode depender de um único canal de notificação | Até 3 tentativas pelo WhatsApp, com espera crescente de 1 e 2 segundos, e envio por e-mail se todas falharem; o número de tentativas e o intervalo são configuráveis |
| RNF09 | Nenhum registro pode ser apagado fisicamente nas operações comuns | Exclusão lógica: o registro é marcado como inativo e deixa de aparecer nas listagens, preservando o histórico |
| RNF10 | A aplicação não pode exibir uma falha sem tratamento | Toda exceção não prevista resulta em resposta HTTP 500 no mesmo formato dos demais erros |
| **Desempenho** | | |
| RNF11 | O webhook deve responder à Meta sem esperar o processamento da mensagem, pois a Meta reenvia eventos quando a resposta demora | O webhook apenas valida a assinatura e interpreta o conteúdo; a abertura da ordem de serviço e a resposta ao usuário ocorrem em segundo plano |
| RNF12 | As listagens não podem carregar todos os registros de uma vez | Paginação em todas as listagens, com 10 ou 20 itens por página como padrão |
| **Usabilidade** | | |
| RNF13 | Abrir um chamado pelo WhatsApp deve ser simples para quem nunca usou o sistema | Uma única mensagem abre o chamado, sem cadastro prévio e sem conversa em várias etapas; mensagens fora do formato recebem as instruções de uso |
| RNF14 | Textos, datas e arquivos devem seguir o padrão brasileiro | Mensagens em português, datas no fuso America/Fortaleza e CSV com ponto e vírgula, codificação UTF-8 e vírgula decimal, compatível com o Excel |
| RNF15 | Os erros devem ser previsíveis para quem consome a API | Respostas de erro no formato RFC 7807 (Problem Details), com códigos coerentes: 400 para regra de negócio, 401 para login inválido, 403 para acesso negado, 404 para recurso inexistente e 422 para dados inválidos |
| **Manutenibilidade** | | |
| RNF16 | Toda mudança no banco de dados deve ser versionada e reproduzível | Migrações sequenciais com Flyway (V1 a V19); o Hibernate apenas valida o mapeamento e nunca altera o esquema |
| RNF17 | O código deve seguir uma organização única para todos os módulos | Um pacote por domínio, cada um com entidade, repositório, serviço, controlador, DTOs e mapeador |
| RNF18 | As regras críticas devem ter testes automatizados | 159 testes automatizados, unitários com Mockito e um teste de contexto executado contra o MySQL |
| **Portabilidade e interoperabilidade** | | |
| RNF19 | O backend deve ser independente da interface de usuário | API REST com JSON, documentada em OpenAPI e acessível pelo Swagger UI; o frontend em React é um projeto separado |
| RNF20 | A aplicação deve ser executada em outro ambiente sem alteração de código | Banco de dados, porta, diretório de arquivos e integrações configurados por variáveis de ambiente; backend em Java 21 com Spring Boot 4.1 e banco MySQL |
| **Privacidade e conformidade** | | |
| RNF21 | O sistema deve reduzir as cópias de dados pessoais, em consonância com a LGPD | Relatórios em PDF gerados em memória e nunca gravados em disco ou no banco; política de privacidade publicada, exigida pela Meta para a integração |
| RNF22 | Um solicitante não pode ver chamados de outra pessoa pelo WhatsApp | A consulta de situação mostra apenas as ordens de serviço cujo solicitante tem o mesmo telefone da mensagem; números sem histórico não geram cadastro na consulta |

## Quadro 4: Regras de negócio

| ID | Regra | Requisitos relacionados |
|---|---|---|
| RN01 | O número de tombamento é único: dois bens não podem ter o mesmo número | RF07 |
| RN02 | Ao abrir uma ordem de serviço vinculada a um bem, o bem passa para a situação em manutenção. Quando a ordem é encerrada ou cancelada, o bem volta para em uso | RF12, RF14 e RF15 |
| RN03 | A atribuição de um técnico coloca a ordem em andamento, e nenhuma ordem pode ficar em andamento sem técnico atribuído | RF13 e RF14 |
| RN04 | Uma ordem só chega à situação resolvida pelo encerramento, que exige o laudo técnico | RF15 |
| RN05 | Ordens resolvidas ou canceladas não aceitam nova mudança de situação nem novo encerramento | RF14 e RF15 |
| RN06 | Toda mudança de situação, inclusive a abertura, gera um registro no histórico da ordem e dispara as notificações | RF18 e RF23 |
| RN07 | As notificações vão para o solicitante e para o técnico atribuído; no encerramento, também para quem encerrou. Ninguém recebe a mesma notificação duas vezes e usuários inativos são ignorados | RF23 |
| RN08 | O WhatsApp só entrega mensagens de texto livre até 24 horas depois da última mensagem enviada pelo destinatário. Fora desse prazo o envio falha e a notificação segue por e-mail | RF23 e RF24 |
| RN09 | No chamado aberto pelo WhatsApp, o setor é localizado pelo nome e o bem pelo número de tombamento. Se algum dos dois não existir, nada é criado e o solicitante recebe a explicação do erro. A prioridade é sempre média e o canal fica registrado como WhatsApp | RF20 e RF22 |
| RN10 | Um número desconhecido que abre chamado pelo WhatsApp é cadastrado automaticamente como solicitante. Na consulta de situação isso não ocorre: o número apenas recebe o aviso de que não há histórico | RF20 e RF21 |
| RN11 | O autor registrado na auditoria e no histórico da ordem é sempre o usuário autenticado na requisição. Ações sem usuário autenticado, como as vindas do WhatsApp, são registradas como SYSTEM | RF18 e RF33 |
| RN12 | Registros inativados deixam de aparecer nas listagens e consultas, mas permanecem no banco de dados para fins de histórico | RF03 a RF11 |
| RN13 | Um bem é indicado como candidato a substituição quando o custo acumulado das ordens resolvidas atinge o percentual informado do valor de aquisição. A depreciação é linear, por meses completos, sem valor residual e com vida útil padrão de 10 anos | RF29 e RF30 |
| RN14 | Os períodos dos filtros e relatórios usam datas inclusivas, interpretadas no fuso America/Fortaleza | RF16 e RF26 a RF34 |

## Quadro 5: Relação entre os objetivos específicos e os requisitos

Os objetivos usam a redação exata da seção 4.2 do TCC. Os requisitos de acesso (RF01 a RF03) e de segurança (RNF01 a RNF06) não aparecem vinculados a um objetivo isolado porque sustentam todos eles.

| Objetivo específico | Requisitos funcionais | Requisitos não funcionais |
|---|---|---|
| Realizar o cadastro e o controle dos bens patrimoniais da instituição | RF04 a RF11 | RNF09, RNF12 e RNF16 |
| Permitir o registro e acompanhamento das ordens de serviço | RF12 a RF16 e RF18 | RNF07, RNF10 e RNF15 |
| Facilitar a comunicação entre os usuários e o setor responsável pela manutenção | RF20 a RF24 | RNF08, RNF13 e RNF14 |
| Integrar o sistema ao WhatsApp para envio de solicitações e notificações | RF19 a RF25 | RNF03, RNF07, RNF11 e RNF22 |
| Organizar o histórico de manutenção dos bens patrimoniais | RF10, RF17, RF18, RF33 e RF34 | RNF09 |
| Contribuir para maior agilidade na identificação e resolução de problemas relacionados ao patrimônio | RF13 a RF15, RF20, RF23 e RF30 | RNF08, RNF11 e RNF13 |
| Disponibilizar informações que auxiliem na tomada de decisões administrativas | RF26 a RF32 | RNF06, RNF14 e RNF21 |

## Fora desta versão

Ficaram como possibilidades de continuidade: a baixa patrimonial formal (motivo, data e responsável), a conversa em várias etapas pelo WhatsApp com confirmação antes da abertura do chamado e o monitoramento automático da integração com o WhatsApp no painel administrativo.
