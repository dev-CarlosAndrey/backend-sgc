package br.com.fpo.sgc.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SearchTermTest {

    @Test
    void buscaVaziaVoltaNuloParaNaoFiltrar() {
        assertNull(SearchTerm.toLikePattern(null));
        assertNull(SearchTerm.toLikePattern(""));
        assertNull(SearchTerm.toLikePattern("   "));
    }

    @Test
    void montaPadraoContemEmMinusculasSemEspacosNasPontas() {
        assertEquals("%notebook dell%", SearchTerm.toLikePattern("  Notebook DELL "));
    }

    @Test
    void escapaCuringasDigitadosPeloUsuario() {
        assertEquals("%100!%%", SearchTerm.toLikePattern("100%"));
        assertEquals("%a!_b%", SearchTerm.toLikePattern("a_b"));
        assertEquals("%ok!!%", SearchTerm.toLikePattern("ok!"));
    }
}
