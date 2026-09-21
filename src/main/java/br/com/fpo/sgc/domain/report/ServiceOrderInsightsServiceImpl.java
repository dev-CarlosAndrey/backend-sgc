package br.com.fpo.sgc.domain.report;

import br.com.fpo.sgc.common.exception.BusinessException;
import br.com.fpo.sgc.common.util.CsvWriter;
import br.com.fpo.sgc.common.util.PeriodRange;
import br.com.fpo.sgc.domain.report.ReportFilters.Period;
import br.com.fpo.sgc.domain.report.dto.*;
import br.com.fpo.sgc.domain.serviceorder.ServiceOrder;
import br.com.fpo.sgc.domain.serviceorder.ServiceOrderPriority;
import br.com.fpo.sgc.domain.serviceorder.ServiceOrderRepository;
import br.com.fpo.sgc.domain.serviceorder.ServiceOrderStatus;
import br.com.fpo.sgc.domain.serviceorder.statuslog.ServiceOrderStatusLog;
import br.com.fpo.sgc.domain.serviceorder.statuslog.ServiceOrderStatusLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static br.com.fpo.sgc.domain.report.ReportMath.*;

@Service
@RequiredArgsConstructor
public class ServiceOrderInsightsServiceImpl implements ServiceOrderInsightsService {

    private static final int DEFAULT_MONTHS = 12;
    private static final int MAX_MONTHS = 60;

    private final ServiceOrderRepository serviceOrderRepository;
    private final ServiceOrderStatusLogRepository statusLogRepository;

    // ---------- evolução mensal ----------

    @Override
    @Transactional(readOnly = true)
    public MonthlyServiceOrdersReportDTO monthly(Period filter) {
        LocalDate today = LocalDate.now(PeriodRange.BUSINESS_ZONE);
        LocalDate to = filter.to() != null ? filter.to() : today;
        LocalDate from = filter.from() != null ? filter.from() : to.minusMonths(DEFAULT_MONTHS - 1L);
        YearMonth first = YearMonth.from(from);
        YearMonth last = YearMonth.from(to);
        if (first.isAfter(last)) {
            throw new BusinessException("A data inicial não pode ser posterior à data final.");
        }
        if (first.until(last, ChronoUnit.MONTHS) >= MAX_MONTHS) {
            throw new BusinessException("O período do relatório mensal é limitado a " + MAX_MONTHS + " meses.");
        }

        // Meses inteiros: do dia 1 do primeiro ao último dia do último mês.
        PeriodRange range = PeriodRange.of(first.atDay(1), last.atEndOfMonth());

        Map<YearMonth, long[]> counts = new TreeMap<>();
        Map<YearMonth, BigDecimal> cost = new HashMap<>();
        for (YearMonth m = first; !m.isAfter(last); m = m.plusMonths(1)) {
            counts.put(m, new long[2]);
        }

        // Abertas no período (por abertura) e resolvidas no período (por conclusão).
        for (ServiceOrder so : serviceOrderRepository.findForReport(range.from(), range.to(), null, null,
                null, null, filter.companyId())) {
            counts.get(monthOf(so.getCreatedAt()))[0]++;
        }
        for (ServiceOrder so : serviceOrderRepository.findForReport(null, null, range.from(), range.to(),
                null, ServiceOrderStatus.RESOLVED, filter.companyId())) {
            YearMonth month = monthOf(so.getClosedAt());
            counts.get(month)[1]++;
            if (so.getCost() != null) {
                cost.merge(month, so.getCost(), BigDecimal::add);
            }
        }

        List<MonthlyServiceOrderRowDTO> rows = new ArrayList<>();
        counts.forEach((month, c) ->
                rows.add(new MonthlyServiceOrderRowDTO(month.toString(), c[0], c[1],
                        cost.getOrDefault(month, BigDecimal.ZERO))));

        return new MonthlyServiceOrdersReportDTO(
                rows.stream().mapToLong(MonthlyServiceOrderRowDTO::opened).sum(),
                rows.stream().mapToLong(MonthlyServiceOrderRowDTO::resolved).sum(),
                rows);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] monthlyCsv(Period filter) {
        List<List<String>> rows = monthly(filter).months().stream().map(r -> List.of(
                r.month(), String.valueOf(r.opened()), String.valueOf(r.resolved()), money(r.cost()))).toList();
        return CsvWriter.toBytes(List.of("Mês", "Abertas", "Concluídas", "Custo das concluídas"), rows);
    }

    // ---------- tempo por etapa ----------

    @Override
    @Transactional(readOnly = true)
    public StageTimeReportDTO stageTimes(Period filter) {
        PeriodRange range = PeriodRange.of(filter.from(), filter.to());
        List<ServiceOrderStatusLog> logs = statusLogRepository.findForReport(range.from(), range.to(), filter.companyId());

        // Agrupa por OS mantendo a ordem cronológica devolvida pela consulta.
        Map<Long, List<ServiceOrderStatusLog>> byOrder = new LinkedHashMap<>();
        for (ServiceOrderStatusLog log : logs) {
            byOrder.computeIfAbsent(log.getServiceOrder().getId(), id -> new ArrayList<>()).add(log);
        }

        Map<ServiceOrderStatus, List<Duration>> stageSamples = new EnumMap<>(ServiceOrderStatus.class);
        Map<ServiceOrderPriority, PriorityAccumulator> priorities = new EnumMap<>(ServiceOrderPriority.class);

        for (List<ServiceOrderStatusLog> history : byOrder.values()) {
            ServiceOrder order = history.get(0).getServiceOrder();
            PriorityAccumulator acc = priorities.computeIfAbsent(order.getPriority(), p -> new PriorityAccumulator());
            acc.orders++;

            // Tempo em cada status = intervalo até a mudança seguinte; a etapa atual (sem saída) não conta.
            boolean firstStageRecorded = false;
            for (int i = 0; i + 1 < history.size(); i++) {
                Duration stay = Duration.between(history.get(i).getCreatedAt(), history.get(i + 1).getCreatedAt());
                stageSamples.computeIfAbsent(history.get(i).getNewStatus(), s -> new ArrayList<>()).add(stay);
                if (!firstStageRecorded) {
                    acc.firstResponse.add(stay);
                    firstStageRecorded = true;
                }
            }
            ServiceOrderStatusLog last = history.get(history.size() - 1);
            if (last.getNewStatus() == ServiceOrderStatus.RESOLVED) {
                acc.resolution.add(Duration.between(history.get(0).getCreatedAt(), last.getCreatedAt()));
            }
        }

        List<StageTimeRowDTO> stages = new ArrayList<>();
        for (ServiceOrderStatus status : ServiceOrderStatus.values()) {
            List<Duration> samples = stageSamples.getOrDefault(status, List.of());
            // RESOLVED e CANCELLED são estados finais: nunca têm "tempo na etapa".
            if (status == ServiceOrderStatus.RESOLVED || status == ServiceOrderStatus.CANCELLED) {
                continue;
            }
            stages.add(new StageTimeRowDTO(status.name(), samples.size(), averageHours(samples),
                    samples.stream().max(Comparator.naturalOrder()).map(ReportMath::hours).orElse(null)));
        }
        String bottleneck = stages.stream().filter(s -> s.avgHours() != null)
                .max(Comparator.comparingDouble(StageTimeRowDTO::avgHours))
                .map(StageTimeRowDTO::status).orElse(null);

        List<PriorityTimeRowDTO> byPriority = new ArrayList<>();
        for (ServiceOrderPriority priority : ServiceOrderPriority.values()) {
            PriorityAccumulator acc = priorities.get(priority);
            byPriority.add(acc == null
                    ? new PriorityTimeRowDTO(priority.name(), 0, null, null)
                    : new PriorityTimeRowDTO(priority.name(), acc.orders,
                            averageHours(acc.firstResponse), averageHours(acc.resolution)));
        }
        return new StageTimeReportDTO(byOrder.size(), bottleneck, stages, byPriority);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] stageTimesCsv(Period filter) {
        StageTimeReportDTO report = stageTimes(filter);
        List<List<String>> rows = new ArrayList<>();
        report.stages().forEach(s -> rows.add(List.of("Etapa", stageLabel(s.status()), String.valueOf(s.samples()),
                num(s.avgHours()), num(s.maxHours()))));
        report.byPriority().forEach(p -> rows.add(List.of("Prioridade", priorityLabel(p.priority()),
                String.valueOf(p.orders()), num(p.avgFirstResponseHours()), num(p.avgResolutionHours()))));
        return CsvWriter.toBytes(List.of("Tipo", "Item", "Quantidade",
                "Média (h) / 1º atendimento (h)", "Máx. (h) / Resolução média (h)"), rows);
    }

    // ---------- apoio ----------

    private static YearMonth monthOf(Instant instant) {
        return YearMonth.from(instant.atZone(PeriodRange.BUSINESS_ZONE));
    }

    private static String num(Double value) {
        return value != null ? String.valueOf(value).replace('.', ',') : "";
    }

    private static String stageLabel(String status) {
        return switch (ServiceOrderStatus.valueOf(status)) {
            case OPEN -> "Aberta";
            case IN_PROGRESS -> "Em andamento";
            case RESOLVED -> "Concluída";
            case CANCELLED -> "Cancelada";
        };
    }

    private static String priorityLabel(String priority) {
        return switch (ServiceOrderPriority.valueOf(priority)) {
            case LOW -> "Baixa";
            case MEDIUM -> "Média";
            case HIGH -> "Alta";
            case URGENT -> "Urgente";
        };
    }

    private static final class PriorityAccumulator {
        private long orders;
        private final List<Duration> firstResponse = new ArrayList<>();
        private final List<Duration> resolution = new ArrayList<>();
    }
}
