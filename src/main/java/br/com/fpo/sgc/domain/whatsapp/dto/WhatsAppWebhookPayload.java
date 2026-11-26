package br.com.fpo.sgc.domain.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Payload enviado pela Meta ao webhook do WhatsApp Cloud API (POST). Um evento traz `messages` (mensagem
 * recebida de um contato) ou `statuses` (confirmação de entrega/leitura/falha de uma mensagem enviada por
 * nós), nunca os dois ao mesmo tempo. Campos não mapeados aqui (outros tipos de mensagem, `errors`,
 * `context`...) são ignorados.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WhatsAppWebhookPayload(String object, List<Entry> entry) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Entry(String id, List<Change> changes) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Change(String field, Value value) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Value(
            List<IncomingMessage> messages,
            List<MessageStatus> statuses,
            List<Contact> contacts) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record IncomingMessage(String from, String id, String timestamp, String type, TextBody text) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TextBody(String body) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MessageStatus(
            String id,
            String status,
            String timestamp,
            @JsonProperty("recipient_id") String recipientId,
            List<StatusError> errors) {}

    // Presente nos status "failed": código e descrição do erro de entrega (ex.: 131047, fora da janela de 24 h).
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StatusError(Integer code, String title, String message) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Contact(@JsonProperty("wa_id") String waId, Profile profile) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Profile(String name) {}
}
