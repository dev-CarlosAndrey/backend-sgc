package br.com.fpo.sgc.common.util;

import org.junit.jupiter.api.Test;

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
