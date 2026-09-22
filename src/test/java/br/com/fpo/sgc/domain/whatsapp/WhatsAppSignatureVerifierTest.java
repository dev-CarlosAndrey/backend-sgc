package br.com.fpo.sgc.domain.whatsapp;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WhatsAppSignatureVerifierTest {

    private static final String APP_SECRET = "segredo-de-teste";
    private static final String BODY = "{\"entry\":[]}";

    @Test
    void aceitaAssinaturaCalculadaCorretamente() {
        WhatsAppSignatureVerifier verifier = new WhatsAppSignatureVerifier(APP_SECRET);

        assertTrue(verifier.isValid(BODY, "sha256=" + hmacSha256(APP_SECRET, BODY)));
    }

    @Test
    void rejeitaAssinaturaComChaveErrada() {
        WhatsAppSignatureVerifier verifier = new WhatsAppSignatureVerifier(APP_SECRET);

        assertFalse(verifier.isValid(BODY, "sha256=" + hmacSha256("outro-segredo", BODY)));
    }

    @Test
    void rejeitaAssinaturaComCorpoAlterado() {
        WhatsAppSignatureVerifier verifier = new WhatsAppSignatureVerifier(APP_SECRET);

        assertFalse(verifier.isValid("{\"entry\":[{}]}", "sha256=" + hmacSha256(APP_SECRET, BODY)));
    }

    @Test
    void rejeitaHeaderAusenteOuSemPrefixo() {
        WhatsAppSignatureVerifier verifier = new WhatsAppSignatureVerifier(APP_SECRET);

        assertFalse(verifier.isValid(BODY, null));
        assertFalse(verifier.isValid(BODY, hmacSha256(APP_SECRET, BODY)));
    }

    @Test
    void semAppSecretConfigurado_sempreRejeita() {
        WhatsAppSignatureVerifier semSegredo = new WhatsAppSignatureVerifier("");
        WhatsAppSignatureVerifier placeholder = new WhatsAppSignatureVerifier("defina_no_ambiente");

        assertFalse(semSegredo.isValid(BODY, "sha256=" + hmacSha256(APP_SECRET, BODY)));
        assertFalse(placeholder.isValid(BODY, "sha256=" + hmacSha256(APP_SECRET, BODY)));
    }

    private static String hmacSha256(String secret, String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
