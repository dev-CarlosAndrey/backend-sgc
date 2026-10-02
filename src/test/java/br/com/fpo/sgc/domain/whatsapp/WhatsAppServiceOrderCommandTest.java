package br.com.fpo.sgc.domain.whatsapp;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class WhatsAppServiceOrderCommandTest {

    @Test
    void mensagemCompleta_extraiSetorBemEProblema() {
        Optional<WhatsAppServiceOrderCommand> parsed = WhatsAppServiceOrderCommand.parse("""
                Abrir OS
                Setor: TI
                Bem: PAT-001
                Problema: Notebook não liga""");

        assertTrue(parsed.isPresent());
        assertEquals("TI", parsed.get().sectorName());
        assertEquals("PAT-001", parsed.get().assetTagNumber());
        assertEquals("Notebook não liga", parsed.get().problemDescription());
    }

    @Test
    void semCampoBem_assetTagNumberFicaNulo() {
        Optional<WhatsAppServiceOrderCommand> parsed = WhatsAppServiceOrderCommand.parse("""
                Setor: Financeiro
                Problema: Ar-condicionado não gela""");

        assertTrue(parsed.isPresent());
        assertNull(parsed.get().assetTagNumber());
    }

    @Test
    void problemaComVariasLinhas_juntaTudo() {
        Optional<WhatsAppServiceOrderCommand> parsed = WhatsAppServiceOrderCommand.parse("""
                Setor: TI
                Problema: Notebook não liga
                Já tentei trocar a tomada
                Luz do carregador nem acende""");

        assertTrue(parsed.get().problemDescription().contains("trocar a tomada"));
        assertTrue(parsed.get().problemDescription().contains("nem acende"));
    }

    @Test
    void semSetor_naoReconhece() {
        assertTrue(WhatsAppServiceOrderCommand.parse("Problema: algo quebrou").isEmpty());
    }

    @Test
    void semProblema_naoReconhece() {
        assertTrue(WhatsAppServiceOrderCommand.parse("Setor: TI").isEmpty());
    }

    @Test
    void mensagemComumSemFormato_naoReconhece() {
        assertTrue(WhatsAppServiceOrderCommand.parse("oi, bom dia, tudo bem?").isEmpty());
    }

    @Test
    void textoNulo_naoReconhece() {
        assertTrue(WhatsAppServiceOrderCommand.parse(null).isEmpty());
    }
}
