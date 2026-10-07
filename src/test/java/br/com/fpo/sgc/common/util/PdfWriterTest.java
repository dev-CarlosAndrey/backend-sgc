package br.com.fpo.sgc.common.util;

import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PdfWriterTest {

    @Test
    void geraPdfValidoComTabela() {
        byte[] pdf = PdfWriter.toBytes("Relatório de Teste",
                List.of("Coluna A", "Coluna B"),
                List.of(List.of("1", "2"), List.of("3", "4")),
                false);

        assertTrue(pdf.length > 0);
        assertEquals("%PDF", new String(pdf, 0, 4));
    }

    // Cabeçalhos longos e conhecidos (os mesmos usados pelos relatórios reais) saem abreviados no PDF —
    // só aqui, o CSV continua com o nome completo — pra não quebrar no meio da palavra numa coluna estreita.
    @Test
    void cabecalhosConhecidos_saemAbreviadosNoTextoDoPdf() throws Exception {
        byte[] pdf = PdfWriter.toBytes("Relatório de Ordens de Serviço",
                List.of("Código", "Título", "Prioridade", "Técnico", "Solicitante"),
                List.of(List.of("7", "Teste", "Baixa", "admin", "admin")),
                true);

        String text = extractText(pdf);

        assertTrue(text.contains("Cód."), "esperava 'Cód.' no PDF, texto extraído: " + text);
        assertTrue(text.contains("Prior."));
        assertTrue(text.contains("Téc."));
        assertTrue(text.contains("Solic."));
        assertFalse(text.contains("Código"), "não deveria conter o cabeçalho completo 'Código'");
    }

    @Test
    void cabecalhoDesconhecido_naoEhAlterado() throws Exception {
        byte[] pdf = PdfWriter.toBytes("Teste", List.of("Minha Coluna Sem Abreviação Cadastrada"),
                List.of(List.of("valor")), false);

        String text = extractText(pdf);

        assertTrue(text.contains("Minha Coluna Sem Abreviação Cadastrada"));
    }

    private static String extractText(byte[] pdf) throws Exception {
        PdfReader reader = new PdfReader(pdf);
        try {
            return new PdfTextExtractor(reader).getTextFromPage(1);
        } finally {
            reader.close();
        }
    }

    @Test
    void semLinhas_naoLancaExcecao() {
        byte[] pdf = PdfWriter.toBytes("Vazio", List.of("A"), List.of(), false);

        assertTrue(pdf.length > 0);
        assertEquals("%PDF", new String(pdf, 0, 4));
    }

    @Test
    void paisagem_tambemGeraPdfValido() {
        byte[] pdf = PdfWriter.toBytes("Paisagem", List.of("A", "B", "C"), List.of(List.of("1", "2", "3")), true);

        assertEquals("%PDF", new String(pdf, 0, 4));
    }
}
