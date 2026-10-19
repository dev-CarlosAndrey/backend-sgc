package br.com.fpo.sgc.common.util;

import java.util.Locale;

/**
 * Converte o texto livre de busca (`?q=`) das listagens no padrão usado nas consultas
 * `LOWER(campo) LIKE :q ESCAPE '!'`.
 * <p>
 * Retorna {@code null} para busca vazia, que as consultas tratam como "sem filtro"
 * ({@code :q IS NULL OR ...}). Os curingas {@code %} e {@code _} digitados pelo usuário
 * são escapados com {@code !} para serem buscados literalmente. O escape não usa barra
 * invertida porque o MySQL a trata como caractere de escape dentro de literais.
 */
public final class SearchTerm {

    private SearchTerm() {
    }

    public static String toLikePattern(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        String escaped = q.trim().toLowerCase(Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + escaped + "%";
    }
}
