# Documentação do MVP — SGC

Este diretório reúne a documentação exigida pelas diretrizes de TCC da FPO para a modalidade **III — Proposta de Produto Tecnológico (MVP)** (`docs/apoio/Orientações de TCC para ADS -2026.pdf`), organizada pelas três dimensões que o documento define para essa modalidade.

## Escopo e Viabilidade

> "documentação que justifique a criação do produto, incluindo análise de viabilidade técnica e, quando aplicável, financeira"

- [`ESCOPO_E_VIABILIDADE.md`](ESCOPO_E_VIABILIDADE.md) — justificativa do produto, escopo (dentro/fora do MVP), viabilidade técnica e financeira.

## Ciclo de Vida do Sistema

> "demonstração do domínio das etapas de levantamento de requisitos, modelagem, implementação, testes e implantação"

| Etapa | Documento |
|---|---|
| Levantamento de requisitos | [`PLANO_IMPLEMENTACAO_MVP.md`](PLANO_IMPLEMENTACAO_MVP.md) — requisitos funcionais (RF) e não funcionais, rastreados ao TCC, com status de implementação de cada item. |
| Modelagem | [`DIAGRAMAS_UML.md`](DIAGRAMAS_UML.md) (classes, ER) e [`PLANO_IMPLEMENTACAO_MVP.md`](PLANO_IMPLEMENTACAO_MVP.md) (seção 5, modelo de dados). |
| Implementação | [`DOCUMENTACAO_TECNICA_TCC.md`](DOCUMENTACAO_TECNICA_TCC.md) — arquitetura, stack, convenções, funcionalidades implementadas. |
| Testes | [`GUIA_DE_TESTES.md`](GUIA_DE_TESTES.md) — estratégia, cobertura atual (143 testes) e testes manuais realizados. |
| Implantação | [`GUIA_DE_IMPLANTACAO.md`](GUIA_DE_IMPLANTACAO.md) — build, variáveis de ambiente, execução, considerações para produção real. |

## Documentação Técnica

> "elaboração de manuais de uso, diagramas (incluindo UML), documentação de APIs e demais artefatos pertinentes"

- **Manuais de uso**: [`MANUAL_DO_USUARIO.md`](MANUAL_DO_USUARIO.md) — uso do sistema pelo painel e pelo WhatsApp, por perfil de usuário.
- **Diagramas (incluindo UML)**: [`DIAGRAMAS_UML.md`](DIAGRAMAS_UML.md) — casos de uso, classes, entidade-relacionamento, sequência (abertura de OS via WhatsApp e fallback de notificação).
- **Documentação de APIs**: [`API_CONTRATO_FRONTEND.md`](API_CONTRATO_FRONTEND.md) — contrato completo de endpoints, autenticação, paginação, formato de erro; complementado pela documentação interativa do Swagger UI (`/swagger-ui.html`, servida pela própria aplicação).
- **Demais artefatos**:
  - [`CONFIGURACAO_WHATSAPP_CLOUD_API.md`](CONFIGURACAO_WHATSAPP_CLOUD_API.md) — configuração da integração com a Meta.
  - [`PRIVACY_POLICY.md`](PRIVACY_POLICY.md) — política de privacidade (exigida pela Meta para a integração do WhatsApp).

## Relatório técnico descritivo

> "o produto desenvolvido deverá ser acompanhado de um relatório técnico descritivo que contextualiza o problema identificado, apresenta as escolhas tecnológicas adotadas e avalia os resultados obtidos"

Coberto, em conjunto, por [`ESCOPO_E_VIABILIDADE.md`](ESCOPO_E_VIABILIDADE.md) (problema e justificativa) e [`DOCUMENTACAO_TECNICA_TCC.md`](DOCUMENTACAO_TECNICA_TCC.md) (escolhas tecnológicas e resultados).

## Pasta `apoio/`

Material de referência institucional (não é produção do autor): orientações oficiais de TCC da FPO que fundamentam a organização desta pasta.
