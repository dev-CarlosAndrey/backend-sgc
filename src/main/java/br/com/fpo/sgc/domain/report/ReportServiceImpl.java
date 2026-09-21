package br.com.fpo.sgc.domain.report;

import br.com.fpo.sgc.common.util.CsvWriter;
import br.com.fpo.sgc.common.util.PeriodRange;
import br.com.fpo.sgc.domain.asset.Asset;
import br.com.fpo.sgc.domain.asset.AssetRepository;
import br.com.fpo.sgc.domain.asset.AssetStatus;
import br.com.fpo.sgc.domain.report.dto.*;
import br.com.fpo.sgc.domain.serviceorder.ServiceOrder;
import br.com.fpo.sgc.domain.serviceorder.ServiceOrderPeriodField;
import br.com.fpo.sgc.domain.serviceorder.ServiceOrderPriority;
import br.com.fpo.sgc.domain.serviceorder.ServiceOrderRepository;
import br.com.fpo.sgc.domain.serviceorder.ServiceOrderStatus;
import br.com.fpo.sgc.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private static final String NO_CATEGORY = "Sem categoria";
    private static final String NO_TECHNICIAN = "Sem técnico";
    private static final int TOP_ASSETS_LIMIT = 5;
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(PeriodRange.BUSINESS_ZONE);

    private final AssetRepository assetRepository;
    private final ServiceOrderRepository serviceOrderRepository;

    // ---------- RF19: bens por categoria/situação ----------

    @Override
    @Transactional(readOnly = true)
    public AssetReportDTO assetsReport(Long companyId) {
        Map<Long, CategoryAccumulator> categories = new HashMap<>();
        for (Object[] row : assetRepository.countByCategoryAndStatus(companyId)) {
            Long categoryId = (Long) row[0];
            String categoryName = row[1] != null ? (String) row[1] : NO_CATEGORY;
            categories.computeIfAbsent(categoryId, id -> new CategoryAccumulator(categoryName))
                    .add((AssetStatus) row[2], (Long) row[3]);
        }

        Map<AssetStatus, Long> statusTotals = new EnumMap<>(AssetStatus.class);
        List<CategoryReportRowDTO> rows = new ArrayList<>();
        categories.forEach((categoryId, acc) -> {
            acc.byStatus.forEach((status, count) -> statusTotals.merge(status, count, Long::sum));
            rows.add(new CategoryReportRowDTO(categoryId, acc.name, acc.total(), acc.statusMap()));
        });
        // Maior categoria primeiro; empate por nome.
        rows.sort(Comparator.comparingLong(CategoryReportRowDTO::total).reversed()
                .thenComparing(CategoryReportRowDTO::categoryName));

        List<LabelCountDTO> byStatus = Arrays.stream(AssetStatus.values())
                .map(status -> new LabelCountDTO(status.name(), statusTotals.getOrDefault(status, 0L)))
                .toList();
        long total = byStatus.stream().mapToLong(LabelCountDTO::total).sum();

        return new AssetReportDTO(total, byStatus, rows);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] assetsReportCsv(Long companyId) {
        AssetReportDTO report = assetsReport(companyId);

        List<String> header = new ArrayList<>(List.of("Categoria", "Total"));
        Arrays.stream(AssetStatus.values()).forEach(status -> header.add(label(status)));

        List<List<String>> rows = new ArrayList<>();
        for (CategoryReportRowDTO row : report.byCategory()) {
            List<String> line = new ArrayList<>(List.of(row.categoryName(), String.valueOf(row.total())));
            Arrays.stream(AssetStatus.values()).forEach(status -> line.add(String.valueOf(row.byStatus().get(status.name()))));
            rows.add(line);
        }
        List<String> totals = new ArrayList<>(List.of("Total geral", String.valueOf(report.total())));
        report.byStatus().forEach(item -> totals.add(String.valueOf(item.total())));
        rows.add(totals);

        return CsvWriter.toBytes(header, rows);
    }

    // ---------- RF20: ordens de serviço por período/técnico/status ----------

    @Override
    @Transactional(readOnly = true)
    public ServiceOrderReportDTO serviceOrdersReport(ServiceOrderReportFilter filter) {
        List<ServiceOrder> orders = fetch(filter);

        long resolved = orders.stream().filter(so -> so.getStatus() == ServiceOrderStatus.RESOLVED).count();
        BigDecimal totalCost = orders.stream()
                .map(ServiceOrder::getCost).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<ServiceOrderStatus, Long> byStatus = new EnumMap<>(ServiceOrderStatus.class);
        Map<ServiceOrderPriority, Long> byPriority = new EnumMap<>(ServiceOrderPriority.class);
        Map<Long, TechnicianAccumulator> byTechnician = new HashMap<>();
        Map<Long, AssetMaintenanceRowDTO> assets = new HashMap<>();

        for (ServiceOrder so : orders) {
            byStatus.merge(so.getStatus(), 1L, Long::sum);
            byPriority.merge(so.getPriority(), 1L, Long::sum);

            User technician = so.getTechnician();
            byTechnician.computeIfAbsent(technician != null ? technician.getId() : null,
                    id -> new TechnicianAccumulator(technician != null ? technician.getName() : NO_TECHNICIAN)).add(so);

            Asset asset = so.getAsset();
            if (asset != null) {
                assets.merge(asset.getId(),
                        new AssetMaintenanceRowDTO(asset.getId(), asset.getTagNumber(), asset.getName(), 1),
                        (a, b) -> new AssetMaintenanceRowDTO(a.assetId(), a.tagNumber(), a.name(), a.total() + 1));
            }
        }

        List<TechnicianReportRowDTO> technicianRows = new ArrayList<>();
        byTechnician.forEach((id, acc) -> technicianRows.add(
                new TechnicianReportRowDTO(id, acc.name, acc.total, acc.resolved, averageHours(acc.durations))));
        technicianRows.sort(Comparator.comparingLong(TechnicianReportRowDTO::total).reversed()
                .thenComparing(TechnicianReportRowDTO::technicianName));

        List<AssetMaintenanceRowDTO> topAssets = assets.values().stream()
                .sorted(Comparator.comparingLong(AssetMaintenanceRowDTO::total).reversed()
                        .thenComparing(AssetMaintenanceRowDTO::tagNumber))
                .limit(TOP_ASSETS_LIMIT)
                .toList();

        List<Duration> resolutionTimes = orders.stream().map(ReportServiceImpl::resolutionTime).filter(Objects::nonNull).toList();

        return new ServiceOrderReportDTO(
                orders.size(),
                resolved,
                totalCost,
                averageHours(resolutionTimes),
                Arrays.stream(ServiceOrderStatus.values())
                        .map(s -> new LabelCountDTO(s.name(), byStatus.getOrDefault(s, 0L))).toList(),
                Arrays.stream(ServiceOrderPriority.values())
                        .map(p -> new LabelCountDTO(p.name(), byPriority.getOrDefault(p, 0L))).toList(),
                technicianRows,
                topAssets);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] serviceOrdersCsv(ServiceOrderReportFilter filter) {
        List<String> header = List.of("Código", "Título", "Status", "Prioridade", "Canal", "Solicitante", "Técnico",
                "Bem (tombamento)", "Setor", "Abertura", "Conclusão", "Custo");

        List<List<String>> rows = fetch(filter).stream().map(so -> List.of(
                String.valueOf(so.getId()),
                so.getTitle(),
                label(so.getStatus()),
                label(so.getPriority()),
                so.getChannel() != null ? so.getChannel().name() : "",
                so.getRequester().getName(),
                so.getTechnician() != null ? so.getTechnician().getName() : "",
                so.getAsset() != null ? so.getAsset().getTagNumber() : "",
                so.getSector().getName(),
                DATE_TIME.format(so.getCreatedAt()),
                so.getClosedAt() != null ? DATE_TIME.format(so.getClosedAt()) : "",
                so.getCost() != null ? so.getCost().setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',') : ""
        )).toList();

        return CsvWriter.toBytes(header, rows);
    }

    // ---------- apoio ----------

    // Datas inclusivas; field decide se o período vale para a abertura ou para a conclusão da ordem.
    private List<ServiceOrder> fetch(ServiceOrderReportFilter filter) {
        PeriodRange range = PeriodRange.of(filter.from(), filter.to());
        boolean byClosing = filter.field() == ServiceOrderPeriodField.CLOSED;
        return serviceOrderRepository.findForReport(
                byClosing ? null : range.from(), byClosing ? null : range.to(),
                byClosing ? range.from() : null, byClosing ? range.to() : null,
                filter.technicianId(), filter.status(), filter.companyId());
    }

    // Tempo até a conclusão; só existe para ordens RESOLVED (canceladas não contam como atendimento).
    private static Duration resolutionTime(ServiceOrder so) {
        if (so.getStatus() != ServiceOrderStatus.RESOLVED || so.getClosedAt() == null || so.getCreatedAt() == null) {
            return null;
        }
        return Duration.between(so.getCreatedAt(), so.getClosedAt());
    }

    private static Double averageHours(List<Duration> durations) {
        if (durations.isEmpty()) {
            return null;
        }
        double hours = durations.stream().mapToLong(Duration::toMinutes).average().orElse(0) / 60.0;
        return BigDecimal.valueOf(hours).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private static String label(AssetStatus status) {
        return switch (status) {
            case AVAILABLE -> "Disponível";
            case IN_USE -> "Em uso";
            case IN_MAINTENANCE -> "Em manutenção";
            case DISCARDED -> "Baixado";
        };
    }

    private static String label(ServiceOrderStatus status) {
        return switch (status) {
            case OPEN -> "Aberta";
            case IN_PROGRESS -> "Em andamento";
            case RESOLVED -> "Concluída";
            case CANCELLED -> "Cancelada";
        };
    }

    private static String label(ServiceOrderPriority priority) {
        return switch (priority) {
            case LOW -> "Baixa";
            case MEDIUM -> "Média";
            case HIGH -> "Alta";
            case URGENT -> "Urgente";
        };
    }

    private static final class CategoryAccumulator {
        private final String name;
        private final Map<AssetStatus, Long> byStatus = new EnumMap<>(AssetStatus.class);

        private CategoryAccumulator(String name) {
            this.name = name;
        }

        private void add(AssetStatus status, long count) {
            byStatus.merge(status, count, Long::sum);
        }

        private long total() {
            return byStatus.values().stream().mapToLong(Long::longValue).sum();
        }

        // Todas as situações, na ordem do enum, com zero onde não há bens.
        private Map<String, Long> statusMap() {
            Map<String, Long> map = new LinkedHashMap<>();
            for (AssetStatus status : AssetStatus.values()) {
                map.put(status.name(), byStatus.getOrDefault(status, 0L));
            }
            return map;
        }
    }

    private static final class TechnicianAccumulator {
        private final String name;
        private long total;
        private long resolved;
        private final List<Duration> durations = new ArrayList<>();

        private TechnicianAccumulator(String name) {
            this.name = name;
        }

        private void add(ServiceOrder so) {
            total++;
            if (so.getStatus() == ServiceOrderStatus.RESOLVED) {
                resolved++;
            }
            Duration duration = resolutionTime(so);
            if (duration != null) {
                durations.add(duration);
            }
        }
    }
}
