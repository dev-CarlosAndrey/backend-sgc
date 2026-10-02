package br.com.fpo.sgc.domain.whatsapp.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WhatsAppWebhookPayloadTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void interpretaMensagemRecebidaDeUmContato() throws Exception {
        String json = """
                {
                  "object": "whatsapp_business_account",
                  "entry": [{
                    "id": "2170572843491811",
                    "changes": [{
                      "field": "messages",
                      "value": {
                        "messaging_product": "whatsapp",
                        "messages": [{
                          "from": "5588999999999",
                          "id": "wamid.abc",
                          "timestamp": "1700000000",
                          "type": "text",
                          "text": { "body": "mensagem do cliente" }
                        }],
                        "contacts": [{ "wa_id": "5588999999999", "profile": { "name": "Nome do contato" } }]
                      }
                    }]
                  }]
                }
                """;

        WhatsAppWebhookPayload payload = objectMapper.readValue(json, WhatsAppWebhookPayload.class);

        WhatsAppWebhookPayload.Value value = payload.entry().get(0).changes().get(0).value();
        assertNull(value.statuses());
        assertEquals(1, value.messages().size());
        assertEquals("5588999999999", value.messages().get(0).from());
        assertEquals("mensagem do cliente", value.messages().get(0).text().body());
        assertEquals("5588999999999", value.contacts().get(0).waId());
        assertEquals("Nome do contato", value.contacts().get(0).profile().name());
    }

    @Test
    void interpretaAtualizacaoDeStatusDeEntrega() throws Exception {
        String json = """
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
                          "id": "wamid.status",
                          "status": "delivered",
                          "timestamp": "1700000000",
                          "recipient_id": "5588999999999"
                        }]
                      }
                    }]
                  }]
                }
                """;

        WhatsAppWebhookPayload payload = objectMapper.readValue(json, WhatsAppWebhookPayload.class);

        WhatsAppWebhookPayload.Value value = payload.entry().get(0).changes().get(0).value();
        assertNull(value.messages());
        assertEquals(1, value.statuses().size());
        assertEquals("delivered", value.statuses().get(0).status());
        assertEquals("5588999999999", value.statuses().get(0).recipientId());
    }

    @Test
    void ignoraCamposDesconhecidosSemFalhar() throws Exception {
        String json = """
                {
                  "object": "whatsapp_business_account",
                  "entry": [{
                    "id": "x",
                    "changes": [{
                      "field": "messages",
                      "value": {
                        "messaging_product": "whatsapp",
                        "messages": [{
                          "from": "5588999999999",
                          "id": "wamid.img",
                          "timestamp": "1700000000",
                          "type": "image",
                          "image": { "id": "media-id", "mime_type": "image/jpeg" }
                        }]
                      }
                    }]
                  }]
                }
                """;

        WhatsAppWebhookPayload payload = objectMapper.readValue(json, WhatsAppWebhookPayload.class);

        assertEquals("image", payload.entry().get(0).changes().get(0).value().messages().get(0).type());
        assertNull(payload.entry().get(0).changes().get(0).value().messages().get(0).text());
    }
}
