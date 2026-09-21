package br.com.fpo.sgc.domain.notification;

import br.com.fpo.sgc.domain.serviceorder.ServiceOrderStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServiceOrderEmailTemplateTest {

    private String render(ServiceOrderStatus status) {
        return ServiceOrderEmailTemplate.render("Maria", "OS #1 - Projetor", "texto", status);
    }

    private long count(String html, String fragment) {
        return html.split(java.util.regex.Pattern.quote(fragment), -1).length - 1;
    }

    @Test
    void corERotuloAcompanhamOStatus() {
        assertTrue(render(ServiceOrderStatus.OPEN).contains("background:#1f3a5f"));
        assertTrue(render(ServiceOrderStatus.OPEN).contains("Chamado aberto"));
        assertTrue(render(ServiceOrderStatus.IN_PROGRESS).contains("background:#d97706"));
        assertTrue(render(ServiceOrderStatus.IN_PROGRESS).contains(">Em andamento<"));
        assertTrue(render(ServiceOrderStatus.RESOLVED).contains("background:#2e8b57"));
        assertTrue(render(ServiceOrderStatus.RESOLVED).contains(">Conclu&iacute;do<"));
        assertTrue(render(ServiceOrderStatus.CANCELLED).contains("background:#c0392b"));
        assertTrue(render(ServiceOrderStatus.CANCELLED).contains(">Cancelado<"));
    }

    @Test
    void barraDeProgressoPreencheAteAEtapaAtual() {
        // barras cinzas = etapas ainda não alcançadas
        assertEquals(2, count(render(ServiceOrderStatus.OPEN), "<td height=\"6\" style=\"background:#d0d7de"));
        assertEquals(1, count(render(ServiceOrderStatus.IN_PROGRESS), "<td height=\"6\" style=\"background:#d0d7de"));
        assertEquals(0, count(render(ServiceOrderStatus.RESOLVED), "<td height=\"6\" style=\"background:#d0d7de"));
    }

    @Test
    void canceladaMostraEtapasAbertaECancelada() {
        String html = render(ServiceOrderStatus.CANCELLED);
        assertTrue(html.contains(">Cancelada<"));
        assertFalse(html.contains(">Conclu&iacute;da<"));
        assertEquals(0, count(html, "<td height=\"6\" style=\"background:#d0d7de"));
    }

    @Test
    void escapaTextoDinamico() {
        String html = ServiceOrderEmailTemplate.render("<b>x</b>", "<i>s</i>", "<script>1</script>", ServiceOrderStatus.OPEN);
        assertFalse(html.contains("<script>"));
        assertFalse(html.contains("<b>x</b>"));
        assertTrue(html.contains("&lt;script&gt;1&lt;/script&gt;"));
    }
}
