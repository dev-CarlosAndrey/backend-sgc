package br.com.fpo.sgc.domain.whatsapp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Valida o header X-Hub-Signature-256 enviado pela Meta em cada chamada do webhook: HMAC-SHA256 do corpo
 * bruto da requisição usando o app secret como chave, no formato "sha256=<hex>". Ver RF14.
 */
@Slf4j
@Component
public class WhatsAppSignatureVerifier {

    private static final String ALGORITHM = "HmacSHA256";
    private static final String PREFIX = "sha256=";

    private final String appSecret;

    public WhatsAppSignatureVerifier(@Value("${whatsapp.cloud-api.app-secret:}") String appSecret) {
        this.appSecret = appSecret;
    }

    public boolean isValid(String rawBody, String signatureHeader) {
        if (appSecret == null || appSecret.isBlank() || appSecret.equals("defina_no_ambiente")) {
            log.warn("WHATSAPP_APP_SECRET não configurado: assinatura do webhook não pode ser validada");
            return false;
        }
        if (signatureHeader == null || !signatureHeader.startsWith(PREFIX)) {
            return false;
        }

        String expected = computeSignature(rawBody);
        String received = signatureHeader.substring(PREFIX.length());
        return constantTimeEquals(expected, received);
    }

    private String computeSignature(String rawBody) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] hash = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Falha ao calcular HMAC-SHA256 da assinatura do webhook", e);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
