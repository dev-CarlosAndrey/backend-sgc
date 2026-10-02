package br.com.fpo.sgc.domain.whatsapp;

import br.com.fpo.sgc.common.util.PeriodRange;
import br.com.fpo.sgc.domain.serviceorder.ServiceOrderStatus;
import br.com.fpo.sgc.domain.serviceorder.dto.ServiceOrderResponseDTO;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Textos padronizados enviados automaticamente a quem escreve para o número comercial do WhatsApp
 * (RF14/RF15/RF16). Mantidos em um só lugar para ficar fácil ajustar o tom/conteúdo sem mexer na lógica de
 * {@link WhatsAppAutoReplyService}.
 */
final class WhatsAppMessageTemplates {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(PeriodRange.BUSINESS_ZONE);

    private static final String OPEN_FORMAT_EXAMPLE = """
            *Abrir OS*
            Setor: <nome do setor>
            Bem: <tombamento, opcional>
            Problema: <descrição do que está acontecendo>""";

    private static final String STATUS_FORMAT_EXAMPLE = """
            *Status* — lista seus últimos chamados
            *Status #<número>* — detalhe de um chamado específico""";

    private WhatsAppMessageTemplates() {
    }

    // Enviada tanto para quem escreve pela primeira vez quanto para quem manda algo fora dos formatos
    // esperados (RF15/RF16) — nos dois casos, a pessoa precisa ver o que pode fazer por aqui.
    static String welcomeMessage() {
        return """
                Olá! 👋 Aqui é o assistente do *SGC* (Sistema de Gerenciamento e Controle Patrimonial) da Faculdade Princesa do Oeste.

                Para abrir uma Ordem de Serviço por aqui, envie uma mensagem neste formato:

                %s

                O setor precisa existir no sistema e, se informar um bem, o tombamento precisa ser exato.

                Para acompanhar um chamado já aberto:

                %s""".formatted(OPEN_FORMAT_EXAMPLE, STATUS_FORMAT_EXAMPLE);
    }

    static String sectorNotFoundMessage(String sectorName) {
        return """
                ❌ Não encontrei o setor "%s" cadastrado no sistema.

                Confira o nome exato do setor (como aparece no painel do SGC) e envie novamente:

                %s""".formatted(sectorName, OPEN_FORMAT_EXAMPLE);
    }

    static String assetNotFoundMessage(String tagNumber) {
        return """
                ❌ Não encontrei nenhum bem com o tombamento "%s".

                Confira o número exato (ou deixe o campo "Bem" em branco se não souber) e envie novamente:

                %s""".formatted(tagNumber, OPEN_FORMAT_EXAMPLE);
    }

    static String serviceOrderCreatedMessage(Long serviceOrderId, String sectorName, boolean hasAsset) {
        String assetLine = hasAsset ? "\nBem vinculado ao chamado." : "";
        return """
                ✅ Chamado *#%d* aberto com sucesso no setor *%s*!%s

                A equipe de manutenção foi notificada. Para acompanhar, envie: Status #%d""".formatted(serviceOrderId, sectorName, assetLine, serviceOrderId);
    }

    static String noServiceOrderHistoryMessage() {
        return """
                Não encontrei nenhum chamado seu por aqui ainda.

                Para abrir um, envie uma mensagem neste formato:

                %s""".formatted(OPEN_FORMAT_EXAMPLE);
    }

    static String serviceOrderNotFoundForPhoneMessage(Long serviceOrderId) {
        return "❌ Não encontrei nenhum chamado *#%d* associado a este número.".formatted(serviceOrderId);
    }

    static String serviceOrderStatusMessage(ServiceOrderResponseDTO serviceOrder) {
        String technicianLine = serviceOrder.technician() != null
                ? "Técnico: " + serviceOrder.technician().name()
                : "Técnico: ainda não atribuído";

        return """
                📋 Chamado *#%d* — %s
                Status: %s
                Setor: %s
                %s
                Aberto em: %s""".formatted(
                serviceOrder.id(),
                serviceOrder.title(),
                statusLabel(serviceOrder.status()),
                serviceOrder.sector().name(),
                technicianLine,
                DATE_TIME.format(serviceOrder.createdAt()));
    }

    static String serviceOrderListMessage(List<ServiceOrderResponseDTO> serviceOrders) {
        String lines = serviceOrders.stream()
                .map(so -> "#%d - %s - %s".formatted(so.id(), statusLabel(so.status()), so.title()))
                .collect(Collectors.joining("\n"));

        return """
                📋 Seus últimos chamados:
                %s

                Para ver o detalhe de um deles, envie: Status #<número>""".formatted(lines);
    }

    private static String statusLabel(ServiceOrderStatus status) {
        return switch (status) {
            case OPEN -> "Aberta";
            case IN_PROGRESS -> "Em andamento";
            case RESOLVED -> "Concluída";
            case CANCELLED -> "Cancelada";
        };
    }
}
