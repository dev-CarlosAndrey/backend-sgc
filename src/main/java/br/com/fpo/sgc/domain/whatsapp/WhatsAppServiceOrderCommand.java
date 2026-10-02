package br.com.fpo.sgc.domain.whatsapp;

import java.util.Locale;
import java.util.Optional;

/**
 * Interpreta o texto de uma mensagem de WhatsApp no formato de abertura de OS (RF15):
 * <pre>
 * Abrir OS
 * Setor: &lt;nome do setor&gt;
 * Bem: &lt;tombamento, opcional&gt;
 * Problema: &lt;descrição&gt;
 * </pre>
 * Parser propositalmente simples (campos identificados por rótulo, em qualquer lugar do texto, mas
 * esperados nessa ordem — linhas após "Problema:" são todas tratadas como parte da descrição, então um
 * "Setor:"/"Bem:" depois do "Problema:" não seria reconhecido). Suficiente para o uso por linha de comando
 * via WhatsApp sem exigir um fluxo de várias mensagens (decisão: minimizar o número de mensagens trocadas).
 */
record WhatsAppServiceOrderCommand(String sectorName, String assetTagNumber, String problemDescription) {

    private static final String SECTOR_LABEL = "setor:";
    private static final String ASSET_LABEL = "bem:";
    private static final String PROBLEM_LABEL = "problema:";

    static Optional<WhatsAppServiceOrderCommand> parse(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return Optional.empty();
        }

        String sector = null;
        String tagNumber = null;
        StringBuilder problem = null;

        for (String line : rawText.split("\\R")) {
            String trimmed = line.trim();

            if (problem != null) {
                problem.append('\n').append(trimmed);
                continue;
            }

            String lower = trimmed.toLowerCase(Locale.ROOT);
            if (lower.startsWith(SECTOR_LABEL)) {
                sector = trimmed.substring(SECTOR_LABEL.length()).trim();
            } else if (lower.startsWith(ASSET_LABEL)) {
                tagNumber = trimmed.substring(ASSET_LABEL.length()).trim();
            } else if (lower.startsWith(PROBLEM_LABEL)) {
                problem = new StringBuilder(trimmed.substring(PROBLEM_LABEL.length()).trim());
            }
        }

        if (sector == null || sector.isBlank() || problem == null || problem.toString().isBlank()) {
            return Optional.empty();
        }

        return Optional.of(new WhatsAppServiceOrderCommand(
                sector,
                (tagNumber == null || tagNumber.isBlank()) ? null : tagNumber,
                problem.toString().trim()));
    }
}
