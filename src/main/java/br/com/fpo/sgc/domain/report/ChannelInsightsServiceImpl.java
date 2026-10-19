package br.com.fpo.sgc.domain.report;

import br.com.fpo.sgc.common.util.CsvWriter;
import br.com.fpo.sgc.common.util.PdfWriter;
import br.com.fpo.sgc.common.util.PeriodRange;
import br.com.fpo.sgc.common.util.TableData;
import br.com.fpo.sgc.domain.notification.Notification;
import br.com.fpo.sgc.domain.notification.NotificationChannel;
import br.com.fpo.sgc.domain.notification.NotificationRepository;
import br.com.fpo.sgc.domain.notification.NotificationStatus;
import br.com.fpo.sgc.domain.report.ReportFilters.Period;
import br.com.fpo.sgc.domain.report.dto.*;
import br.com.fpo.sgc.domain.serviceorder.ServiceOrder;
import br.com.fpo.sgc.domain.serviceorder.ServiceOrderChannel;
import br.com.fpo.sgc.domain.serviceorder.ServiceOrderRepository;
import br.com.fpo.sgc.domain.serviceorder.ServiceOrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.YearMonth;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static br.com.fpo.sgc.domain.report.ReportMath.averageHours;
import static br.com.fpo.sgc.domain.report.ReportMath.percent;

@Service
@RequiredArgsConstructor
public class ChannelInsightsServiceImpl implements ChannelInsightsService {

    private static final int TOP_ERRORS = 5;
    private static final int MAX_ERROR_LABEL = 150;
    // Erros da Graph API/SendGrid chegam como JSON; o texto útil está no primeiro campo "message".
    private static final Pattern JSON_MESSAGE = Pattern.compile("\"message\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    private final ServiceOrderRepository serviceOrderRepository;
    private final NotificationRepository notificationRepository;

    // ---------- OS por canal de abertura ----------

    @Override
    @Transactional(readOnly = true)
    public ChannelReportDTO channels(Period filter) {
        PeriodRange range = PeriodRange.of(filter.from(), filter.to());
        List<ServiceOrder> orders = serviceOrderRepository.findForReport(range.from(), range.to(), null, null,
                null, null, filter.companyId());

        Map<ServiceOrderChannel, List<ServiceOrder>> byChannel = new EnumMap<>(ServiceOrderChannel.class);
        Map<YearMonth, long[]> byMonth = new TreeMap<>();
        for (ServiceOrder so : orders) {
            ServiceOrderChannel channel = channelOf(so);
            byChannel.computeIfAbsent(channel, c -> new ArrayList<>()).add(so);
            long[] month = byMonth.computeIfAbsent(
                    YearMonth.from(so.getCreatedAt().atZone(PeriodRange.BUSINESS_ZONE)), m -> new long[2]);
            month[channel == ServiceOrderChannel.WHATSAPP ? 1 : 0]++;
        }

        List<ChannelRowDTO> channels = new ArrayList<>();
        for (ServiceOrderChannel channel : ServiceOrderChannel.values()) {
            List<ServiceOrder> list = byChannel.getOrDefault(channel, List.of());
            long resolved = count(list, ServiceOrderStatus.RESOLVED);
            long cancelled = count(list, ServiceOrderStatus.CANCELLED);
            List<Duration> resolution = list.stream()
                    .filter(so -> so.getStatus() == ServiceOrderStatus.RESOLVED && so.getClosedAt() != null)
                    .map(so -> Duration.between(so.getCreatedAt(), so.getClosedAt())).toList();
            channels.add(new ChannelRowDTO(channel.name(), list.size(), percent(list.size(), orders.size()),
                    resolved, cancelled, list.size() - resolved - cancelled, averageHours(resolution)));
        }

        List<ChannelMonthRowDTO> months = new ArrayList<>();
        byMonth.forEach((month, c) -> months.add(
                new ChannelMonthRowDTO(month.toString(), c[0], c[1], percent(c[1], c[0] + c[1]))));

        return new ChannelReportDTO(orders.size(), channels, months);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] channelsCsv(Period filter) {
        TableData table = channelsTable(filter);
        return CsvWriter.toBytes(table.header(), table.rows());
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] channelsPdf(Period filter) {
        TableData table = channelsTable(filter);
        return PdfWriter.toBytes("Relatório de OS por Canal de Abertura", table.header(), table.rows(), false);
    }

    private TableData channelsTable(Period filter) {
        ChannelReportDTO report = channels(filter);
        List<List<String>> rows = new ArrayList<>();
        report.channels().forEach(c -> rows.add(List.of("Canal", channelLabel(c.channel()), String.valueOf(c.total()),
                num(c.sharePercent()), String.valueOf(c.resolved()), String.valueOf(c.cancelled()),
                String.valueOf(c.pending()), num(c.avgResolutionHours()))));
        report.months().forEach(m -> {
            rows.add(List.of("Mês", m.month() + " - Painel", String.valueOf(m.panel()),
                    num(percent(m.panel(), m.panel() + m.whatsapp())), "", "", "", ""));
            rows.add(List.of("Mês", m.month() + " - WhatsApp", String.valueOf(m.whatsapp()),
                    num(m.whatsappPercent()), "", "", "", ""));
        });
        return new TableData(List.of("Tipo", "Item", "Quantidade", "Percentual (%)", "Resolvidas", "Canceladas",
                "Pendentes", "Resolução média (h)"), rows);
    }

    // ---------- entrega das notificações ----------

    @Override
    @Transactional(readOnly = true)
    public NotificationReportDTO notifications(Period filter) {
        PeriodRange range = PeriodRange.of(filter.from(), filter.to());
        List<Notification> attempts = notificationRepository.findForReport(range.from(), range.to(), filter.companyId());

        // Tentativas por canal
        Map<NotificationChannel, long[]> perChannel = new EnumMap<>(NotificationChannel.class);
        for (Notification n : attempts) {
            long[] c = perChannel.computeIfAbsent(n.getChannel(), k -> new long[2]);
            c[n.getDeliveryStatus() == NotificationStatus.SENT ? 0 : 1]++;
        }
        List<NotificationChannelRowDTO> channels = new ArrayList<>();
        for (NotificationChannel channel : NotificationChannel.values()) {
            long[] c = perChannel.getOrDefault(channel, new long[2]);
            channels.add(new NotificationChannelRowDTO(channel.name(), c[0] + c[1], c[0], c[1], percent(c[0], c[0] + c[1])));
        }

        // Desfecho de cada notificação
        List<List<Notification>> deliveries = groupIntoDeliveries(attempts);
        Map<Outcome, Long> outcomes = new EnumMap<>(Outcome.class);
        long triedWhatsApp = 0;
        for (List<Notification> delivery : deliveries) {
            outcomes.merge(outcomeOf(delivery), 1L, Long::sum);
            if (delivery.get(0).getChannel() == NotificationChannel.WHATSAPP) {
                triedWhatsApp++;
            }
        }
        List<LabelCountDTO> byOutcome = Arrays.stream(Outcome.values())
                .map(o -> new LabelCountDTO(o.name(), outcomes.getOrDefault(o, 0L))).toList();
        long delivered = deliveries.size() - outcomes.getOrDefault(Outcome.NOT_DELIVERED, 0L);

        List<LabelCountDTO> topErrors = attempts.stream()
                .filter(n -> n.getDeliveryStatus() == NotificationStatus.FAILED)
                .map(n -> errorLabel(n.getErrorMessage()))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(TOP_ERRORS)
                .map(e -> new LabelCountDTO(e.getKey(), e.getValue())).toList();

        return new NotificationReportDTO(attempts.size(), deliveries.size(), delivered,
                percent(delivered, deliveries.size()),
                percent(outcomes.getOrDefault(Outcome.EMAIL_FALLBACK, 0L), triedWhatsApp),
                channels, byOutcome, topErrors);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] notificationsCsv(Period filter) {
        TableData table = notificationsTable(filter);
        return CsvWriter.toBytes(table.header(), table.rows());
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] notificationsPdf(Period filter) {
        TableData table = notificationsTable(filter);
        return PdfWriter.toBytes("Relatório de Entrega das Notificações", table.header(), table.rows(), false);
    }

    private TableData notificationsTable(Period filter) {
        NotificationReportDTO r = notifications(filter);
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("Resumo", "Notificações", String.valueOf(r.totalNotifications()), ""));
        rows.add(List.of("Resumo", "Entregues", String.valueOf(r.delivered()), num(r.deliveryRatePercent())));
        rows.add(List.of("Resumo", "Fallback para e-mail (das que tentaram WhatsApp)",
                String.valueOf(count(r.byOutcome(), Outcome.EMAIL_FALLBACK)), num(r.fallbackRatePercent())));
        rows.add(List.of("Resumo", "Tentativas de envio", String.valueOf(r.totalAttempts()), ""));
        r.channels().forEach(c -> {
            rows.add(List.of("Canal", channelLabel(c.channel()) + " - enviadas", String.valueOf(c.sent()),
                    num(c.successRatePercent())));
            rows.add(List.of("Canal", channelLabel(c.channel()) + " - falhas", String.valueOf(c.failed()), ""));
        });
        r.byOutcome().forEach(o -> rows.add(List.of("Desfecho", outcomeLabel(o.key()), String.valueOf(o.total()),
                num(percent(o.total(), r.totalNotifications())))));
        r.topErrors().forEach(e -> rows.add(List.of("Erro", e.key(), String.valueOf(e.total()), "")));
        return new TableData(List.of("Tipo", "Item", "Quantidade", "Percentual (%)"), rows);
    }

    /**
     * Reagrupa as tentativas (linhas de tb_notifications) em notificações. O NotificationServiceImpl grava,
     * para cada destinatário de um evento, as tentativas de WhatsApp (1..n) e, se todas falharem, as de
     * e-mail (1..n). Como a tabela não guarda o id do evento, uma notificação nova começa quando muda a
     * OS/destinatário ou quando aparece uma tentativa 1 que não seja o fallback logo após falha de WhatsApp.
     */
    static List<List<Notification>> groupIntoDeliveries(List<Notification> attempts) {
        List<List<Notification>> deliveries = new ArrayList<>();
        List<Notification> current = null;
        Notification previous = null;
        for (Notification n : attempts) {
            boolean sameTarget = previous != null
                    && Objects.equals(previous.getServiceOrder().getId(), n.getServiceOrder().getId())
                    && Objects.equals(previous.getRecipient().getId(), n.getRecipient().getId());
            boolean isFallback = sameTarget && n.getChannel() == NotificationChannel.EMAIL
                    && previous.getChannel() == NotificationChannel.WHATSAPP
                    && previous.getDeliveryStatus() == NotificationStatus.FAILED;
            boolean startsNew = current == null || !sameTarget || (n.getAttempt() == 1 && !isFallback);
            if (startsNew) {
                current = new ArrayList<>();
                deliveries.add(current);
            }
            current.add(n);
            previous = n;
        }
        return deliveries;
    }

    private static Outcome outcomeOf(List<Notification> delivery) {
        boolean triedWhatsApp = delivery.get(0).getChannel() == NotificationChannel.WHATSAPP;
        for (Notification n : delivery) {
            if (n.getDeliveryStatus() != NotificationStatus.SENT) {
                continue;
            }
            if (n.getChannel() == NotificationChannel.WHATSAPP) {
                return n.getAttempt() == 1 ? Outcome.WHATSAPP_FIRST_ATTEMPT : Outcome.WHATSAPP_AFTER_RETRY;
            }
            return triedWhatsApp ? Outcome.EMAIL_FALLBACK : Outcome.EMAIL_ONLY;
        }
        return Outcome.NOT_DELIVERED;
    }

    enum Outcome {
        WHATSAPP_FIRST_ATTEMPT,
        WHATSAPP_AFTER_RETRY,
        EMAIL_FALLBACK,
        EMAIL_ONLY,
        NOT_DELIVERED
    }

    // ---------- apoio ----------

    static String errorLabel(String error) {
        if (error == null || error.isBlank()) {
            return "Erro sem detalhe";
        }
        Matcher m = JSON_MESSAGE.matcher(error);
        String label = (m.find() ? m.group(1) : error).strip();
        return label.length() > MAX_ERROR_LABEL ? label.substring(0, MAX_ERROR_LABEL) + "..." : label;
    }

    // OS antigas (antes da coluna channel) são tratadas como abertas pelo painel, o padrão da migração V13.
    private static ServiceOrderChannel channelOf(ServiceOrder so) {
        return so.getChannel() != null ? so.getChannel() : ServiceOrderChannel.PANEL;
    }

    private static long count(List<ServiceOrder> orders, ServiceOrderStatus status) {
        return orders.stream().filter(so -> so.getStatus() == status).count();
    }

    private static long count(List<LabelCountDTO> rows, Outcome outcome) {
        return rows.stream().filter(r -> r.key().equals(outcome.name())).mapToLong(LabelCountDTO::total).sum();
    }

    private static String num(Double value) {
        return value != null ? String.valueOf(value).replace('.', ',') : "";
    }

    private static String channelLabel(String channel) {
        return switch (channel) {
            case "PANEL" -> "Painel";
            case "WHATSAPP" -> "WhatsApp";
            case "EMAIL" -> "E-mail";
            default -> channel;
        };
    }

    private static String outcomeLabel(String outcome) {
        return switch (Outcome.valueOf(outcome)) {
            case WHATSAPP_FIRST_ATTEMPT -> "WhatsApp na 1ª tentativa";
            case WHATSAPP_AFTER_RETRY -> "WhatsApp após retentativa";
            case EMAIL_FALLBACK -> "E-mail (fallback)";
            case EMAIL_ONLY -> "Só e-mail (sem WhatsApp)";
            case NOT_DELIVERED -> "Não entregue";
        };
    }
}
