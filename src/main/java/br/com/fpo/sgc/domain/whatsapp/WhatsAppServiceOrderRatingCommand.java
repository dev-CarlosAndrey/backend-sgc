package br.com.fpo.sgc.domain.whatsapp;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interpreta o texto de uma mensagem de WhatsApp como avaliação de atendimento: "Nota #12 5" ou "Nota 12 5"
 * (OS 12, nota 5). Segue o mesmo estilo dos outros comandos: uma mensagem só, sem conversa em etapas.
 * Uma mensagem que começa com "Nota" mas não está no formato (ex.: nota 7, sem número da OS) vira um comando
 * inválido, para o solicitante receber o formato certo em vez da mensagem de boas-vindas.
 */
record WhatsAppServiceOrderRatingCommand(Long serviceOrderId, Integer score) {

    private static final Pattern VALID = Pattern.compile("^nota\\s*#?\\s*(\\d+)\\s+([1-5])\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ATTEMPT = Pattern.compile("^nota\\b.*", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    static Optional<WhatsAppServiceOrderRatingCommand> parse(String rawText) {
        if (rawText == null) {
            return Optional.empty();
        }
        String text = rawText.trim();
        Matcher matcher = VALID.matcher(text);
        if (matcher.matches()) {
            return Optional.of(new WhatsAppServiceOrderRatingCommand(Long.valueOf(matcher.group(1)), Integer.valueOf(matcher.group(2))));
        }
        return ATTEMPT.matcher(text).matches() ? Optional.of(new WhatsAppServiceOrderRatingCommand(null, null)) : Optional.empty();
    }

    boolean isValid() {
        return serviceOrderId != null && score != null;
    }
}
