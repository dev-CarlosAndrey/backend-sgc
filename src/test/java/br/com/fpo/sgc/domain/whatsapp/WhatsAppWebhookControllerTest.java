package br.com.fpo.sgc.domain.whatsapp;

import br.com.fpo.sgc.domain.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class WhatsAppWebhookControllerTest {

    private static final String VERIFY_TOKEN = "meu-verify-token";
    private static final String APP_SECRET = "segredo";

    private static final String MENSAGEM_RECEBIDA = """
            {
              "object": "whatsapp_business_account",
              "entry": [{
                "id": "2170572843491811",
                "changes": [{
                  "field": "messages",
                  "value": {
                    "messaging_product": "whatsapp",
                    "messages": [{
                      "from": "558899999999",
                      "id": "wamid.abc",
                      "timestamp": "1700000000",
                      "type": "text",
                      "text": { "body": "Quero abrir um chamado" }
                    }],
                    "contacts": [{ "wa_id": "558899999999", "profile": { "name": "Maria" } }]
                  }
                }]
              }]
            }
            """;

    private static final String ATUALIZACAO_DE_STATUS = """
            {
              "object": "whatsapp_business_account",
              "entry": [{
                "id": "2170572843491811",
                "changes": [{
                  "field": "messages",
                  "value": {
                    "messaging_product": "whatsapp",
                    "statuses": [{
                      "id": "wamid.def",
                      "status": "delivered",
                      "timestamp": "1700000000",
                      "recipient_id": "558899999999"
                    }]
                  }
                }]
              }]
            }
            """;

    private static final String FALHA_DE_ENTREGA = """
            {
              "object": "whatsapp_business_account",
              "entry": [{
                "id": "2170572843491811",
                "changes": [{
                  "field": "messages",
                  "value": {
                    "messaging_product": "whatsapp",
                    "statuses": [{
                      "id": "wamid.ghi",
                      "status": "failed",
                      "timestamp": "1700000000",
                      "recipient_id": "558899999999",
                      "errors": [{ "code": 131047, "title": "Re-engagement message", "message": "Re-engagement message" }]
                    }]
                  }
                }]
              }]
            }
            """;

    private final WhatsAppAutoReplyService autoReplyService = mock(WhatsAppAutoReplyService.class);
    private final WhatsAppInboundMessageRegistry inboundMessageRegistry = mock(WhatsAppInboundMessageRegistry.class);
    private final NotificationService notificationService = mock(NotificationService.class);

    private final WhatsAppWebhookController controller = controller(VERIFY_TOKEN);

    @BeforeEach
    void setUp() {
        when(inboundMessageRegistry.registerIfNew(any(), any())).thenReturn(true);
    }

    private WhatsAppWebhookController controller(String verifyToken) {
        return new WhatsAppWebhookController(new WhatsAppSignatureVerifier(APP_SECRET), autoReplyService,
                inboundMessageRegistry, notificationService, verifyToken);
    }

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
        WhatsAppWebhookController semToken = controller("");

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

    @Test
    void receive_aceitaMensagemRecebidaComAssinaturaValida() {
        ResponseEntity<Void> response = controller.receive(sign(MENSAGEM_RECEBIDA), MENSAGEM_RECEBIDA);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void receive_mensagemRecebida_repassaParaOServicoDeRespostaAutomatica() {
        controller.receive(sign(MENSAGEM_RECEBIDA), MENSAGEM_RECEBIDA);

        verify(autoReplyService).handleIncomingMessage(
                eq("558899999999"), eq("Maria"), eq("Quero abrir um chamado"));
    }

    @Test
    void receive_aceitaAtualizacaoDeStatusComAssinaturaValida() {
        ResponseEntity<Void> response = controller.receive(sign(ATUALIZACAO_DE_STATUS), ATUALIZACAO_DE_STATUS);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void receive_apenasAtualizacaoDeStatus_naoDisparaRespostaAutomatica() {
        controller.receive(sign(ATUALIZACAO_DE_STATUS), ATUALIZACAO_DE_STATUS);

        verifyNoInteractions(autoReplyService);
    }

    @Test
    void receive_mensagemJaProcessada_naoProcessaDeNovo() {
        when(inboundMessageRegistry.registerIfNew("wamid.abc", "558899999999")).thenReturn(false);

        ResponseEntity<Void> response = controller.receive(sign(MENSAGEM_RECEBIDA), MENSAGEM_RECEBIDA);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verifyNoInteractions(autoReplyService);
    }

    @Test
    void receive_statusFailed_acionaFallbackComDescricaoDoErro() {
        ResponseEntity<Void> response = controller.receive(sign(FALHA_DE_ENTREGA), FALHA_DE_ENTREGA);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(notificationService).handleAsyncDeliveryFailure(eq("wamid.ghi"), contains("131047"));
    }

    @Test
    void receive_statusDelivered_naoAcionaFallback() {
        controller.receive(sign(ATUALIZACAO_DE_STATUS), ATUALIZACAO_DE_STATUS);

        verify(notificationService, never()).handleAsyncDeliveryFailure(any(), any());
    }

    @Test
    void receive_payloadMalformadoComAssinaturaValida_aindaAssimConfirma() {
        String malformado = "{isto nao e json valido";

        ResponseEntity<Void> response = controller.receive(sign(malformado), malformado);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    private static String sign(String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(APP_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return "sha256=" + HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
