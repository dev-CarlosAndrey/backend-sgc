package br.com.fpo.sgc.domain.notification;

import br.com.fpo.sgc.domain.serviceorder.ServiceOrderStatus;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

/**
 * Layout HTML dos e-mails da OS: a cor do cabeçalho, o rótulo e a barra de progresso acompanham o status.
 * Estilos inline e tabelas, por compatibilidade com clientes de e-mail. Todo texto dinâmico é escapado
 * (o laudo técnico, por exemplo, é digitado por usuários).
 */
final class ServiceOrderEmailTemplate {

    private static final String GREY = "#d0d7de";

    private ServiceOrderEmailTemplate() {
    }

    private record Theme(String color, String label) {
    }

    static String render(String recipientName, String subject, String message, ServiceOrderStatus status) {
        Theme theme = theme(status);
        List<String> steps = status == ServiceOrderStatus.CANCELLED
                ? List.of("Aberta", "Cancelada")
                : List.of("Aberta", "Em andamento", "Concluída");
        int current = currentStep(status);

        return """
                <div style="font-family:Arial,Helvetica,sans-serif;background:#f4f5f7;padding:24px">
                  <div style="max-width:560px;margin:0 auto;background:#ffffff;border-radius:8px;overflow:hidden;border:1px solid #e1e4e8">
                    <div style="background:%1$s;color:#ffffff;padding:16px 24px">
                      <div style="font-size:12px;letter-spacing:.5px;opacity:.9">SGC &middot; Controle Patrimonial</div>
                      <div style="font-size:20px;font-weight:bold;margin-top:4px">%2$s</div>
                    </div>
                    <div style="padding:24px;color:#24292f">
                      <h2 style="margin:0 0 16px;font-size:18px">%3$s</h2>
                      %4$s
                      <p style="margin:20px 0 16px;font-size:14px">Olá, %5$s.</p>
                      <p style="margin:0;font-size:15px;line-height:1.5">%6$s</p>
                    </div>
                    <div style="padding:12px 24px;background:#f6f8fa;color:#6a737d;font-size:12px">
                      Mensagem automática do sistema de controle patrimonial da FPO. Não responda este e-mail.
                    </div>
                  </div>
                </div>
                """.formatted(
                theme.color(),
                HtmlUtils.htmlEscape(theme.label()),
                HtmlUtils.htmlEscape(subject),
                progressBar(steps, current, theme.color()),
                HtmlUtils.htmlEscape(recipientName),
                HtmlUtils.htmlEscape(message).replace("\n", "<br>"));
    }

    private static Theme theme(ServiceOrderStatus status) {
        return switch (status) {
            case OPEN -> new Theme("#1f3a5f", "Chamado aberto");
            case IN_PROGRESS -> new Theme("#d97706", "Em andamento");
            case RESOLVED -> new Theme("#2e8b57", "Concluído");
            case CANCELLED -> new Theme("#c0392b", "Cancelado");
        };
    }

    // Índice da etapa atual dentro da lista de etapas exibida para o status.
    private static int currentStep(ServiceOrderStatus status) {
        return switch (status) {
            case OPEN -> 0;
            case IN_PROGRESS, CANCELLED -> 1;
            case RESOLVED -> 2;
        };
    }

    // Etapas até a atual ficam na cor do status; as demais, em cinza.
    private static String progressBar(List<String> steps, int current, String color) {
        StringBuilder bars = new StringBuilder();
        StringBuilder labels = new StringBuilder();
        for (int i = 0; i < steps.size(); i++) {
            boolean reached = i <= current;
            bars.append("<td height=\"6\" style=\"background:").append(reached ? color : GREY)
                    .append(";border-radius:3px\"></td>");
            labels.append("<td align=\"center\" style=\"padding-top:6px;font-size:12px;color:")
                    .append(i == current ? color : "#6a737d").append(";font-weight:")
                    .append(i == current ? "bold" : "normal").append("\">")
                    .append(HtmlUtils.htmlEscape(steps.get(i))).append("</td>");
        }
        return "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"4\" style=\"table-layout:fixed\"><tr>"
                + bars + "</tr><tr>" + labels + "</tr></table>";
    }
}
