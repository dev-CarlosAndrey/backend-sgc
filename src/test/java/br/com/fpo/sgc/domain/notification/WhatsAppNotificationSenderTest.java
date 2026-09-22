package br.com.fpo.sgc.domain.notification;

import br.com.fpo.sgc.domain.serviceorder.ServiceOrderStatus;
import br.com.fpo.sgc.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class WhatsAppNotificationSenderTest {

    private static final String TOKEN = "token-de-teste";
    private static final String PHONE_NUMBER_ID = "1234567890";

    private MockRestServiceServer server;
    private WhatsAppNotificationSender sender;
    private final User user = User.builder().name("Maria").phone("(85) 99999-0000").build();

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        sender = new WhatsAppNotificationSender(builder, "https://graph.facebook.com/v22.0", TOKEN, PHONE_NUMBER_ID);
    }

    @Test
    void enviaPayloadCorretoParaAGraphApi_removendoFormatacaoDoTelefone() {
        server.expect(requestTo("https://graph.facebook.com/v22.0/" + PHONE_NUMBER_ID + "/messages"))
                .andExpect(method(POST))
                .andExpect(header("Authorization", "Bearer " + TOKEN))
                .andExpect(jsonPath("$.messaging_product").value("whatsapp"))
                .andExpect(jsonPath("$.to").value("85999990000"))
                .andExpect(jsonPath("$.type").value("text"))
                .andExpect(jsonPath("$.text.body").value("OS #1 - Projetor\n\nmensagem"))
                .andRespond(withStatus(OK));

        sender.send(user, "OS #1 - Projetor", "mensagem", ServiceOrderStatus.OPEN);

        server.verify();
    }

    @Test
    void respostaDeErro_propagaExcecao() {
        server.expect(requestTo("https://graph.facebook.com/v22.0/" + PHONE_NUMBER_ID + "/messages"))
                .andRespond(withStatus(UNAUTHORIZED));

        assertThrows(RestClientResponseException.class, () -> sender.send(user, "s", "m", ServiceOrderStatus.OPEN));
    }

    @Test
    void canSend_exigeConfiguracaoETelefoneDoDestinatario() {
        assertTrue(sender.canSend(user));
        assertFalse(sender.canSend(User.builder().name("Sem telefone").build()));

        WhatsAppNotificationSender semToken = new WhatsAppNotificationSender(
                RestClient.builder(), "https://graph.facebook.com/v22.0", "", PHONE_NUMBER_ID);
        assertFalse(semToken.canSend(user));

        WhatsAppNotificationSender placeholder = new WhatsAppNotificationSender(
                RestClient.builder(), "https://graph.facebook.com/v22.0", "defina_no_ambiente", PHONE_NUMBER_ID);
        assertFalse(placeholder.canSend(user));
    }
}
