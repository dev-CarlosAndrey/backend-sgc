package br.com.fpo.sgc.domain.whatsapp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class WhatsAppServiceOrderRatingCommandTest {

    @ParameterizedTest
    @ValueSource(strings = {"Nota #12 5", "nota 12 5", "NOTA #12   5", "  Nota # 12 5  "})
    void formatoValido_extraiOsENota(String text) {
        WhatsAppServiceOrderRatingCommand command = WhatsAppServiceOrderRatingCommand.parse(text).orElseThrow();

        assertTrue(command.isValid());
        assertEquals(12L, command.serviceOrderId());
        assertEquals(5, command.score());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Nota #12 7", "Nota #12 0", "Nota 5", "Nota", "nota #12 cinco"})
    void comecaComNotaForaDoFormato_viraComandoInvalido(String text) {
        WhatsAppServiceOrderRatingCommand command = WhatsAppServiceOrderRatingCommand.parse(text).orElseThrow();

        assertFalse(command.isValid());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Status #12", "oi", "Anotação 12 5", ""})
    void outraMensagem_naoEAvaliacao(String text) {
        assertTrue(WhatsAppServiceOrderRatingCommand.parse(text).isEmpty());
    }

    @Test
    void textoNulo_naoEAvaliacao() {
        assertTrue(WhatsAppServiceOrderRatingCommand.parse(null).isEmpty());
    }
}
