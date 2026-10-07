# Guia de Implantação

> Documenta a etapa de **implantação** do ciclo de vida do sistema, exigida pelas diretrizes de TCC da FPO para a modalidade III (MVP): "demonstração do domínio das etapas de levantamento de requisitos, modelagem, implementação, testes e **implantação**" (`docs/apoio/Orientações de TCC para ADS -2026.pdf`).
>
> Este repositório **não contém** `Dockerfile`, `docker-compose` nem pipeline de CI/CD — o estado real do projeto é execução local/manual, adequada ao estágio de MVP acadêmico. Este guia documenta exatamente esse processo; não descreve infraestrutura que não existe.

## 1. Pré-requisitos

- Java 21 (JDK).
- MySQL acessível (local ou remoto), schema criado (as tabelas são criadas pelo próprio Flyway na primeira inicialização — não é preciso rodar SQL manualmente).
- Opcional: conta SendGrid (notificação por e-mail) e credenciais da WhatsApp Cloud API (ver `CONFIGURACAO_WHATSAPP_CLOUD_API.md`) — sem elas, os respectivos canais ficam desabilitados automaticamente e o restante do sistema funciona normalmente.

## 2. Build

```bash
./mvnw package
```

Gera um jar executável autocontido (Spring Boot embute o Tomcat) em `target/sgc-api-<versão>.jar`.

## 3. Variáveis de ambiente

Toda a configuração de infraestrutura vem de variáveis de ambiente (`src/main/resources/application.yml`), com valores padrão de desenvolvimento quando aplicável. Resumo — detalhamento completo em `../CLAUDE.md`, seção "Ambiente local / configuração":

| Variável | Obrigatória? | Efeito se ausente |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Sim (fora do padrão `localhost:3306/sgc_db` root/root) | Aplicação não sobe (sem banco). |
| `JWT_SECRET` | Recomendada em produção | Usa segredo padrão de desenvolvimento — **nunca usar o padrão fora de ambiente local**. |
| `SENDGRID_API_KEY`, `SENDGRID_FROM_EMAIL` | Não | Canal de e-mail desabilitado; aplicação segue funcionando sem fallback por e-mail. |
| `WHATSAPP_ACCESS_TOKEN`, `WHATSAPP_PHONE_NUMBER_ID`, `WHATSAPP_BUSINESS_ACCOUNT_ID`, `WHATSAPP_WEBHOOK_VERIFY_TOKEN`, `WHATSAPP_APP_SECRET` | Não, mas necessárias para o diferencial do produto | Canal WhatsApp desabilitado (envio e webhook). |
| `UPLOAD_DIR` | Não (padrão `uploads`) | Usa diretório relativo padrão. |
| `PORT` | Não (padrão `8080`) | Usa porta 8080. |

**Nunca versionar segredos** (`SENDGRID_API_KEY`, `WHATSAPP_ACCESS_TOKEN`, `WHATSAPP_APP_SECRET`, `JWT_SECRET` de produção) em `application.yml` nem em commit — configurar via variável de ambiente do ambiente de execução (ex.: run configuration do IntelliJ em desenvolvimento, variáveis de ambiente do serviço em produção).

## 4. Migrações de banco

O Flyway roda automaticamente na inicialização da aplicação (comportamento padrão do Spring Boot) — não é necessário executar nenhum comando manual de migração. Na primeira subida contra um banco vazio, todas as migrações `V1`–`V19` são aplicadas em sequência, incluindo a criação do usuário administrador de bootstrap (`V16`, login `admin` / senha `Admin@123`).

**Checklist pós-primeira-subida**: trocar a senha do usuário `admin` antes de qualquer uso real ou demonstração exposta publicamente.

## 5. Rodando a aplicação

### 5.1 Desenvolvimento

```bash
./mvnw spring-boot:run
```

ou executar `SgcApiApplication` diretamente pela IDE (IntelliJ), configurando as variáveis de ambiente na run configuration.

### 5.2 A partir do jar empacotado

```bash
java -jar target/sgc-api-<versão>.jar
```

As mesmas variáveis de ambiente da seção 3 se aplicam.

## 6. Expondo o webhook do WhatsApp (ambiente de desenvolvimento)

A Meta exige um endpoint HTTPS público para o webhook — em desenvolvimento local, isso é feito com um túnel (ngrok). Passo a passo completo em `CONFIGURACAO_WHATSAPP_CLOUD_API.md`, seção 5. Esse requisito de HTTPS público se mantém em qualquer implantação real (ver seção 7).

## 7. Considerações para uma implantação em produção real (fora do escopo deste MVP acadêmico)

Este MVP foi demonstrado em ambiente local (ver `ESCOPO_E_VIABILIDADE.md`, seção 5). Caso a FPO decida adotar o sistema de fato, os pontos abaixo precisariam ser endereçados — nenhum deles está implementado hoje:

- **Servidor com HTTPS público**: obrigatório para o webhook do WhatsApp funcionar fora de um túnel de desenvolvimento (ex.: VPS + proxy reverso como nginx + certificado TLS, ou um serviço gerenciado com HTTPS nativo).
- **Segredos de produção**: `JWT_SECRET` forte e exclusivo de produção, credenciais reais da WhatsApp Cloud API (não o número de teste), SendGrid em conta paga se o volume ultrapassar o tier gratuito.
- **Banco de dados gerenciado ou com backup**: hoje a instância de MySQL usada em desenvolvimento não tem rotina de backup — necessário antes de qualquer uso com dados reais da instituição.
- **Processo de build/deploy repetível**: não existe pipeline de CI/CD neste repositório; uma implantação real se beneficiaria de automatizar o build (`./mvnw package`) e o deploy, hoje manuais.
- **Monitoramento básico**: o item de robustez já identificado e ainda pendente no próprio MVP — health-check da integração de notificação (ver `PLANO_IMPLEMENTACAO_MVP.md`, seção 9.1) — se tornaria mais relevante em produção contínua.

Essas considerações são descritas aqui como direção para trabalho futuro, não como parte entregue do MVP.
