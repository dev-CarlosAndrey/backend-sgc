package br.com.fpo.sgc.domain.whatsapp;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WhatsAppWebhookControllerTest {

    private static final String VERIFY_TOKEN = "meu-verify-token";

    private final WhatsAppWebhookController controller =
            new WhatsAppWebhookController(new WhatsAppSignatureVerifier("segredo"), VERIFY_TOKEN);

    @Test
    void verify_aceitaDesafioComTokenCorreto() {
        ResponseEntity<String> response = controller.verify("subscribe", VERIFY_TOKEN, "123456");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("123456", response.getBody());
    }

    @Test
    void verify_rejeitaTokenIncorreto() {
        ResponseEntity<String> response = controller.verify("subscribe", "token-errado", "123456");

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void verify_rejeitaModoDiferenteDeSubscribe() {
        ResponseEntity<String> response = controller.verify("unsubscribe", VERIFY_TOKEN, "123456");

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void verify_semVerifyTokenConfigurado_sempreRejeita() {
        WhatsAppWebhookController semToken =
                new WhatsAppWebhookController(new WhatsAppSignatureVerifier("segredo"), "");

        ResponseEntity<String> response = semToken.verify("subscribe", "", "123456");

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void receive_rejeitaAssinaturaInvalida() {
        ResponseEntity<Void> response = controller.receive("sha256=assinatura-invalida", "{}");

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void receive_semHeaderDeAssinatura_rejeita() {
        ResponseEntity<Void> response = controller.receive(null, "{}");

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}
