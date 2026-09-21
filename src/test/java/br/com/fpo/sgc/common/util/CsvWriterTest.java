package br.com.fpo.sgc.common.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvWriterTest {

    private String csv(List<String> header, List<List<String>> rows) {
        return new String(CsvWriter.toBytes(header, rows), StandardCharsets.UTF_8);
    }

    @Test
    void usaBomPontoEVirgulaECrlf() {
        String out = csv(List.of("A", "B"), List.of(List.of("1", "2")));
        assertEquals("﻿A;B\r\n1;2\r\n", out);
    }

    @Test
    void escapaSeparadorAspasEQuebraDeLinha() {
        String out = csv(List.of("H"), List.of(List.of("a;b"), List.of("diz \"oi\""), List.of("l1\nl2")));
        assertTrue(out.contains("\"a;b\""));
        assertTrue(out.contains("\"diz \"\"oi\"\"\""));
        assertTrue(out.contains("\"l1\nl2\""));
    }

    @Test
    void neutralizaFormulas() {
        String out = csv(List.of("H"), List.of(List.of("=1+1"), List.of("@SOMA(A1)"), List.of("+55"), List.of("-2")));
        assertTrue(out.contains("'=1+1"));
        assertTrue(out.contains("'@SOMA(A1)"));
        assertTrue(out.contains("'+55"));
        assertTrue(out.contains("'-2"));
    }

    @Test
    void nuloViraCelulaVazia() {
        assertEquals("﻿H\r\n\r\n", csv(List.of("H"), List.of(java.util.Collections.singletonList(null))));
    }
}
