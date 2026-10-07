# Configuração da Integração WhatsApp Cloud API — sgc-api

> Gerado para uso com Claude CLI dentro do projeto `sgc-api`. Este arquivo resume tudo que já foi feito na Meta, o que falta implementar no código, e os passos de configuração local (ngrok + webhook). Leia também `PLANO_IMPLEMENTACAO_MVP.md` na raiz do projeto para o contexto geral do MVP.

## ⚠️ Regra importante (não mexer ainda)

**Não alterar a escrita oficial do TCC (docx) por causa desta migração.** A decisão de trocar Evolution API → WhatsApp Cloud API só deve ser refletida na redação acadêmica depois que toda a integração (envio + webhook + recebimento) estiver validada de ponta a ponta em ambiente real. Por enquanto, só atualizar arquivos de planejamento/status do projeto (`PLANO_IMPLEMENTACAO_MVP.md`).

## 1. Contexto: por que migramos

Evolution API (Baileys/WPPConnect) foi abandonada por risco real de banimento do número em 2026 (onda de enforcement da Meta contra automações não-oficiais que emulam WhatsApp Web) e por instabilidade estrutural (quebra a cada mudança de protocolo). Optamos pela **WhatsApp Cloud API oficial da Meta** — mais burocrática para configurar, mas sem esse risco, com contrato estável (Graph API).

## 2. O que já está pronto na Meta (não precisa mexer)

- App Meta criado: **SGC API - TCC** (App ID: `925206270282054`)
- Use case: "Connect with customers through WhatsApp" (WhatsApp Business Platform)
- Business Portfolio: "SGC - Controle Patrimonial"
- Número de telefone comercial registrado: linha nova brasileira (eSIM NuCel), status **Registered**
- **Phone Number ID**: `1387156237803772`
- **WhatsApp Business Account ID**: `2170572843491811`
- Método de pagamento cadastrado (cartão virtual) — necessário para mensagens business-initiated (marketing/utility/authentication); mensagens de resposta a cliente (service) têm cota gratuita de 1.000 conversas/mês
- Token de acesso de produção gerado no painel (Step 2 → Send message → Generate token) — **já configurado como variável de ambiente no IntelliJ**, não precisa ser digitado em lugar nenhum do código
- Teste de envio real confirmado: mensagem de template ("Integration Test") chegou com sucesso no WhatsApp pessoal do desenvolvedor, BR → BR (sem a restrição de cross-country que bloqueava o número de teste americano — erro 130497)

## 3. Variáveis de ambiente (já setadas no IntelliJ "Edit Configurations")

O `application.yml` já está preparado com placeholders `${VAR:default}`. Não hardcodar nenhum destes valores no código-fonte — sempre ler via `@Value` ou `@ConfigurationProperties` a partir do `application.yml`.

```
WHATSAPP_CLOUD_API_BASE_URL       (default: https://graph.facebook.com/v22.0)
WHATSAPP_ACCESS_TOKEN             (token de produção — já setado)
WHATSAPP_PHONE_NUMBER_ID          = 1387156237803772
WHATSAPP_BUSINESS_ACCOUNT_ID      = 2170572843491811
WHATSAPP_WEBHOOK_VERIFY_TOKEN     = sgc_webhook_verify_2026   (string arbitrária escolhida por nós — usar exatamente esta, ou a que estiver de fato no application.yml/IntelliJ)
WHATSAPP_APP_SECRET               (App Secret do painel Meta > App settings > Basic — já setado)

SENDGRID_API_KEY                  (já setado — fallback de e-mail)
SENDGRID_FROM_EMAIL               (já setado)
```

Antes de escrever/alterar código, **ler o `application.yml` atual** para confirmar os nomes exatos das propriedades já mapeadas (ex.: `whatsapp.cloud-api.access-token`, `whatsapp.cloud-api.phone-number-id`, etc.) e reaproveitar essa estrutura em vez de criar propriedades novas duplicadas.

## 4. Estado atual do código (já existe — revisar antes de reescrever)

Os seguintes arquivos já existem no projeto e parecem ser de uma tentativa anterior de implementação. **Ler o conteúdo de cada um antes de decidir o que falta**, para não duplicar lógica nem quebrar algo que já funciona:

- `br/com/fpo/sgc/domain/whatsapp/WhatsAppWebhookController.java`
- `br/com/fpo/sgc/domain/whatsapp/WhatsAppSignatureVerifier.java`
- `br/com/fpo/sgc/domain/notification/WhatsAppNotificationSender.java`
- `br/com/fpo/sgc/domain/notification/NotificationSender.java` (interface)
- `br/com/fpo/sgc/domain/notification/NotificationService.java` / `NotificationServiceImpl.java`
- `br/com/fpo/sgc/domain/notification/EmailNotificationSender.java` (fallback SendGrid)
- `br/com/fpo/sgc/domain/notification/Notification.java`, `NotificationChannel.java`, `NotificationStatus.java`, `NotificationRepository.java`
- `br/com/fpo/sgc/domain/notification/ServiceOrderNotificationListener.java`, `ServiceOrderEmailTemplate.java`

### Checklist do que precisa estar implementado/correto

- [ ] **Client HTTP para envio** (`WhatsAppCloudApiClient` ou equivalente dentro de `WhatsAppNotificationSender`): faz `POST https://graph.facebook.com/v22.0/{phone-number-id}/messages` com header `Authorization: Bearer {access-token}` e body no formato:
  ```json
  {
    "messaging_product": "whatsapp",
    "to": "<numero-destino-com-DDI-DDD-sem-simbolos>",
    "type": "template",
    "template": { "name": "<nome_do_template_aprovado>", "language": { "code": "pt_BR" } }
  }
  ```
  ou, para mensagens de texto livre dentro da janela de 24h de atendimento (`type: "text"`, `text.body`).
- [ ] **Verificação do webhook (GET)**: endpoint deve responder ao `GET` que a Meta manda na configuração, comparando `hub.verify_token` com `WHATSAPP_WEBHOOK_VERIFY_TOKEN` e devolvendo `hub.challenge` em texto puro se bater.
- [ ] **Recebimento de eventos (POST)**: endpoint deve aceitar `POST` com o payload da Meta, validar a assinatura via header `X-Hub-Signature-256` usando `WHATSAPP_APP_SECRET` (é isso que `WhatsAppSignatureVerifier` deveria fazer — conferir se está implementado corretamente com HMAC-SHA256).
- [ ] **Parsing do payload**: o payload pode conter `entry[].changes[].value.messages[]` (mensagem recebida do cliente) ou `entry[].changes[].value.statuses[]` (atualização de status: sent/delivered/read/failed). Exemplos reais capturados durante os testes:

  Status de entrega/erro (exemplo real, útil para os testes unitários):
  ```json
  {
    "object": "whatsapp_business_account",
    "entry": [{
      "id": "2170572843491811",
      "changes": [{
        "field": "messages",
        "value": {
          "messaging_product": "whatsapp",
          "metadata": { "display_phone_number": "5588930794 38", "phone_number_id": "1387156237803772" },
          "statuses": [{
            "id": "wamid....",
            "status": "delivered",
            "timestamp": "...",
            "recipient_id": "5588..."
          }]
        }
      }]
    }]
  }
  ```

  Mensagem recebida do cliente (formato esperado — a Meta inclui um array `messages` em vez de `statuses` quando o cliente manda algo):
  ```json
  {
    "object": "whatsapp_business_account",
    "entry": [{
      "changes": [{
        "field": "messages",
        "value": {
          "messages": [{
            "from": "5588...",
            "id": "wamid....",
            "timestamp": "...",
            "type": "text",
            "text": { "body": "mensagem do cliente" }
          }],
          "contacts": [{ "wa_id": "5588...", "profile": { "name": "Nome do contato" } }]
        }
      }]
    }]
  }
  ```
- [ ] **Integração com `NotificationServiceImpl`**: confirmar que o fluxo tenta WhatsApp primeiro (via `WhatsAppNotificationSender`) e cai para `EmailNotificationSender` (SendGrid) em caso de falha, conforme desenhado em `notification.fallback-enabled` no `application.yml`.
- [ ] **Tratamento de erros da API**: mapear pelo menos os erros mais comuns (token expirado, número não registrado, limite de mensagens) para logs claros — não precisa ser exaustivo para o MVP.
- [ ] **Testes**: pelo menos um teste unitário do parsing do webhook e um da assinatura HMAC, se ainda não existirem.

## 5. Rodando localmente com webhook público (ngrok)

O ngrok já está instalado e autenticado na máquina (`ngrok config add-authtoken ...` já executado).

1. Subir a aplicação Spring Boot normalmente (porta padrão `8080`, conforme `server.port` no `application.yml`).
2. Em outro terminal, rodar:
   ```
   ngrok http 8080
   ```
3. Copiar a URL pública HTTPS que aparece na linha "Forwarding" (algo como `https://xxxx.ngrok-free.app`).
4. Ir no painel da Meta: App **SGC API - TCC** → **WhatsApp > Configuration** (ou dentro do fluxo "Step 2. Production setup" → "Configure Webhooks").
5. Preencher:
   - **Callback URL**: `https://xxxx.ngrok-free.app/<caminho-do-endpoint-de-webhook-no-controller>` (confirmar o `@RequestMapping`/`@PostMapping` real do `WhatsAppWebhookController` — ex.: `/api/whatsapp/webhook`)
   - **Verify token**: o mesmo valor de `WHATSAPP_WEBHOOK_VERIFY_TOKEN`
6. Clicar em **Verify and save**. Se o endpoint responder corretamente ao GET de verificação, a Meta confirma e habilita o webhook.
7. Na mesma tela, marcar (subscribe) pelo menos o campo **messages** para começar a receber eventos.
8. Testar: mandar uma mensagem do WhatsApp pessoal para o número comercial (`+55 8893079438`) e verificar se o `WhatsAppWebhookController` recebeu e logou o payload.

**Atenção**: toda vez que o ngrok reiniciar (sem plano pago com domínio fixo), a URL pública muda — será preciso atualizar o Callback URL na Meta de novo.

## 6. Depois de validar tudo

Quando o fluxo completo estiver confirmado (envio + webhook + recebimento funcionando de ponta a ponta):

1. Atualizar `PLANO_IMPLEMENTACAO_MVP.md` marcando a integração como **concluída e validada** (não só "em teste").
2. Só então, avisar para revisarmos juntos a atualização da escrita do TCC (seção 6.6 e seção 9) refletindo o uso real da WhatsApp Cloud API em vez da Evolution API — isso deve ser feito com cuidado para manter o padrão ABNT e as citações acadêmicas já existentes no documento.
