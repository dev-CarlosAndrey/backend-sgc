package br.com.fpo.sgc.domain.whatsapp;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interpreta o texto de uma mensagem de WhatsApp como consulta de status (RF16): a mensagem inteira (sem
 * mais nada) precisa ser só "Status" (lista os últimos chamados do telefone) ou "Status #12"/"Status 12"
 * (detalhe de um chamado específico). Qualquer outra coisa no texto já não é reconhecida como esse comando.
 */
record WhatsAppServiceOrderStatusQueryCommand(Long serviceOrderId) {

    private static final Pattern PATTERN = Pattern.compile("^status(?:\\s*#?\\s*(\\d+))?\\s*$", Pattern.CASE_INSENSITIVE);

    static Optional<WhatsAppServiceOrderStatusQueryCommand> parse(String rawText) {
        if (rawText == null) {
            return Optional.empty();
        }
        Matcher matcher = PATTERN.matcher(rawText.trim());
        if (!matcher.matches()) {
            return Optional.empty();
        }
        String id = matcher.group(1);
        return Optional.of(new WhatsAppServiceOrderStatusQueryCommand(id != null ? Long.valueOf(id) : null));
    }
}
