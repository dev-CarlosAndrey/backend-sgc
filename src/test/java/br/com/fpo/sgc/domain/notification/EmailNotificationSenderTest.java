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
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class EmailNotificationSenderTest {

    private static final String KEY = "SG.chave-de-teste";

    private MockRestServiceServer server;
    private EmailNotificationSender sender;
    private final User user = User.builder().name("Maria").email("maria@fpo.edu.br").build();

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        sender = new EmailNotificationSender(builder, "https://api.sendgrid.com", KEY, "sgc@fpo.edu.br", "SGC");
    }

    @Test
    void canSend_emailSinteticoDoAutocadastroPeloWhatsApp_naoEnvia() {
        User autocadastrado = User.builder().name("Maria").email("whatsapp+5588999999999@sgc.local").build();

        assertFalse(sender.canSend(autocadastrado));
        assertTrue(sender.canSend(user));
    }

    @Test
    void enviaPayloadCorretoParaOSendGrid() {
        server.expect(requestTo("https://api.sendgrid.com/v3/mail/send"))
                .andExpect(method(POST))
                .andExpect(header("Authorization", "Bearer " + KEY))
                .andExpect(jsonPath("$.personalizations[0].to[0].email").value("maria@fpo.edu.br"))
                .andExpect(jsonPath("$.from.email").value("sgc@fpo.edu.br"))
                .andExpect(jsonPath("$.subject").value("OS #1 - Projetor"))
                .andExpect(jsonPath("$.content[0].type").value("text/plain"))
                .andExpect(jsonPath("$.content[0].value").value("mensagem"))
                .andExpect(jsonPath("$.content[1].type").value("text/html"))
                .andExpect(jsonPath("$.content[1].value").value(org.hamcrest.Matchers.containsString("OS #1 - Projetor")))
                .andExpect(jsonPath("$.content[1].value").value(org.hamcrest.Matchers.containsString("Olá, Maria.")))
                .andRespond(withStatus(ACCEPTED));

        sender.send(user, "OS #1 - Projetor", "mensagem", ServiceOrderStatus.OPEN);

        server.verify();
    }

    @Test
    void htmlEscapaTextoDinamico() {
        server.expect(requestTo("https://api.sendgrid.com/v3/mail/send"))
                .andExpect(jsonPath("$.content[1].value").value(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("&lt;script&gt;alert(1)&lt;/script&gt;"),
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<script>")))))
                .andExpect(jsonPath("$.content[0].value").value("<script>alert(1)</script>"))
                .andRespond(withStatus(ACCEPTED));

        sender.send(user, "s", "<script>alert(1)</script>", ServiceOrderStatus.OPEN);

        server.verify();
    }

    @Test
    void respostaDeErro_propagaExcecao() {
        server.expect(requestTo("https://api.sendgrid.com/v3/mail/send")).andRespond(withStatus(UNAUTHORIZED));

        assertThrows(RestClientResponseException.class, () -> sender.send(user, "s", "m", ServiceOrderStatus.OPEN));
    }

    @Test
    void canSend_exigeConfiguracaoEEmailDoDestinatario() {
        assertTrue(sender.canSend(user));
        assertFalse(sender.canSend(User.builder().name("Sem email").build()));

        EmailNotificationSender semChave = new EmailNotificationSender(
                RestClient.builder(), "https://api.sendgrid.com", "", "sgc@fpo.edu.br", "SGC");
        assertFalse(semChave.canSend(user));

        EmailNotificationSender placeholder = new EmailNotificationSender(
                RestClient.builder(), "https://api.sendgrid.com", "sua_chave_sendgrid_aqui", "sgc@fpo.edu.br", "SGC");
        assertFalse(placeholder.canSend(user));
    }
}
