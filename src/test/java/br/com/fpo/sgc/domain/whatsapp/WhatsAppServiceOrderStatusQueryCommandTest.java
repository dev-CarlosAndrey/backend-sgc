package br.com.fpo.sgc.domain.whatsapp;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class WhatsAppServiceOrderStatusQueryCommandTest {

    @Test
    void apenasStatus_semNumero() {
        Optional<WhatsAppServiceOrderStatusQueryCommand> parsed = WhatsAppServiceOrderStatusQueryCommand.parse("Status");

        assertTrue(parsed.isPresent());
        assertNull(parsed.get().serviceOrderId());
    }

    @Test
    void statusComAlmofadaENumero() {
        assertEquals(42L, WhatsAppServiceOrderStatusQueryCommand.parse("Status #42").get().serviceOrderId());
    }

    @Test
    void statusComNumeroSemAlmofada() {
        assertEquals(42L, WhatsAppServiceOrderStatusQueryCommand.parse("status 42").get().serviceOrderId());
    }

    @Test
    void caseInsensitiveEComEspacos() {
        assertTrue(WhatsAppServiceOrderStatusQueryCommand.parse("  STATUS  ").isPresent());
    }

    @Test
    void textoComMaisConteudo_naoReconhece() {
        assertTrue(WhatsAppServiceOrderStatusQueryCommand.parse("Status do meu chamado, por favor").isEmpty());
    }

    @Test
    void mensagemComum_naoReconhece() {
        assertTrue(WhatsAppServiceOrderStatusQueryCommand.parse("bom dia").isEmpty());
    }

    @Test
    void textoNulo_naoReconhece() {
        assertTrue(WhatsAppServiceOrderStatusQueryCommand.parse(null).isEmpty());
    }
}
