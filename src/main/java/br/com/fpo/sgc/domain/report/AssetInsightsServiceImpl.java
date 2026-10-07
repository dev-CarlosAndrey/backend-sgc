package br.com.fpo.sgc.domain.report;

import br.com.fpo.sgc.common.exception.BusinessException;
import br.com.fpo.sgc.common.util.CsvWriter;
import br.com.fpo.sgc.common.util.PdfWriter;
import br.com.fpo.sgc.common.util.PeriodRange;
import br.com.fpo.sgc.common.util.TableData;
import br.com.fpo.sgc.domain.asset.Asset;
import br.com.fpo.sgc.domain.asset.AssetConservationState;
import br.com.fpo.sgc.domain.asset.AssetRepository;
import br.com.fpo.sgc.domain.asset.AssetStatus;
import br.com.fpo.sgc.domain.report.ReportFilters.Critical;
import br.com.fpo.sgc.domain.report.ReportFilters.GroupBy;
import br.com.fpo.sgc.domain.report.ReportFilters.Patrimony;
import br.com.fpo.sgc.domain.report.dto.CriticalAssetRowDTO;
import br.com.fpo.sgc.domain.report.dto.CriticalAssetsReportDTO;
import br.com.fpo.sgc.domain.report.dto.PatrimonyReportDTO;
import br.com.fpo.sgc.domain.report.dto.PatrimonyRowDTO;
import br.com.fpo.sgc.domain.serviceorder.ServiceOrder;
import br.com.fpo.sgc.domain.serviceorder.ServiceOrderRepository;
import br.com.fpo.sgc.domain.serviceorder.ServiceOrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static br.com.fpo.sgc.domain.report.ReportMath.money;

@Service
@RequiredArgsConstructor
public class AssetInsightsServiceImpl implements AssetInsightsService {

    static final String BAD_CONSERVATION = "BAD_CONSERVATION";
    static final String RECURRENT_MAINTENANCE = "RECURRENT_MAINTENANCE";
    private static final String NO_CATEGORY = "Sem categoria";
    private static final Set<AssetConservationState> BAD_STATES =
            EnumSet.of(AssetConservationState.BAD, AssetConservationState.UNSERVICEABLE);

    private final AssetRepository assetRepository;
    private final ServiceOrderRepository serviceOrderRepository;

    // ---------- patrimônio com depreciação ----------

    @Override
    @Transactional(readOnly = true)
    public PatrimonyReportDTO patrimony(Patrimony filter) {
        if (filter.usefulLifeYears() < 1 || filter.usefulLifeYears() > 50) {
            throw new BusinessException("A vida útil deve estar entre 1 e 50 anos.");
        }
        LocalDate asOf = filter.asOf() != null ? filter.asOf() : LocalDate.now(PeriodRange.BUSINESS_ZONE);
        long lifeMonths = filter.usefulLifeYears() * 12L;
        boolean bySector = filter.groupBy() == GroupBy.SECTOR;

        Map<Long, PatrimonyAccumulator> groups = new LinkedHashMap<>();
        for (Asset asset : assetRepository.findForPatrimony(filter.companyId())) {
            if (asset.getStatus() == AssetStatus.DISCARDED) {
                continue;
            }
            Long groupId = bySector ? asset.getSector().getId()
                    : asset.getCategory() != null ? asset.getCategory().getId() : null;
            String groupName = bySector ? asset.getSector().getName()
                    : asset.getCategory() != null ? asset.getCategory().getName() : NO_CATEGORY;
            PatrimonyAccumulator acc = groups.computeIfAbsent(groupId, id -> new PatrimonyAccumulator(groupName));
            acc.assets++;

            if (asset.getAcquisitionValue() == null || asset.getAcquisitionDate() == null) {
                acc.withoutValuation++;
                continue;
            }
            acc.acquisition = acc.acquisition.add(asset.getAcquisitionValue());
            acc.depreciation = acc.depreciation.add(depreciation(asset, asOf, lifeMonths));
        }

        List<PatrimonyRowDTO> rows = new ArrayList<>();
        groups.forEach((id, acc) -> rows.add(new PatrimonyRowDTO(id, acc.name, acc.assets, acc.withoutValuation,
                acc.acquisition, acc.depreciation, acc.acquisition.subtract(acc.depreciation))));
        // Maior valor contábil primeiro; empate por nome.
        rows.sort(Comparator.comparing(PatrimonyRowDTO::bookValue).reversed().thenComparing(PatrimonyRowDTO::groupName));

        BigDecimal acquisition = rows.stream().map(PatrimonyRowDTO::acquisitionValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal depreciation = rows.stream().map(PatrimonyRowDTO::accumulatedDepreciation).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PatrimonyReportDTO(asOf, filter.usefulLifeYears(),
                rows.stream().mapToLong(PatrimonyRowDTO::assets).sum(),
                rows.stream().mapToLong(PatrimonyRowDTO::assetsWithoutValuation).sum(),
                acquisition, depreciation, acquisition.subtract(depreciation), rows);
    }

    // Linear por meses completos, limitada ao valor do bem (bem totalmente depreciado vale zero, nunca negativo).
    private static BigDecimal depreciation(Asset asset, LocalDate asOf, long lifeMonths) {
        long months = Math.max(0, ChronoUnit.MONTHS.between(asset.getAcquisitionDate(), asOf));
        if (months >= lifeMonths) {
            return asset.getAcquisitionValue().setScale(2, RoundingMode.HALF_UP);
        }
        return asset.getAcquisitionValue().multiply(BigDecimal.valueOf(months))
                .divide(BigDecimal.valueOf(lifeMonths), 2, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] patrimonyCsv(Patrimony filter) {
        TableData table = patrimonyTable(filter);
        return CsvWriter.toBytes(table.header(), table.rows());
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] patrimonyPdf(Patrimony filter) {
        TableData table = patrimonyTable(filter);
        return PdfWriter.toBytes("Relatório de Patrimônio com Depreciação", table.header(), table.rows(), true);
    }

    private TableData patrimonyTable(Patrimony filter) {
        PatrimonyReportDTO report = patrimony(filter);
        String groupHeader = filter.groupBy() == GroupBy.SECTOR ? "Setor" : "Categoria";

        List<List<String>> rows = new ArrayList<>();
        report.groups().forEach(g -> rows.add(patrimonyLine(g.groupName(), g.assets(), g.assetsWithoutValuation(),
                g.acquisitionValue(), g.accumulatedDepreciation(), g.bookValue())));
        rows.add(patrimonyLine("Total geral", report.assets(), report.assetsWithoutValuation(),
                report.acquisitionValue(), report.accumulatedDepreciation(), report.bookValue()));

        return new TableData(List.of(groupHeader, "Bens", "Sem valorização", "Valor de aquisição",
                "Depreciação acumulada", "Valor contábil"), rows);
    }

    private static List<String> patrimonyLine(String name, long assets, long without, BigDecimal acquisition,
                                              BigDecimal depreciation, BigDecimal book) {
        return List.of(name, String.valueOf(assets), String.valueOf(without),
                money(acquisition), money(depreciation), money(book));
    }

    // ---------- bens críticos ----------

    @Override
    @Transactional(readOnly = true)
    public CriticalAssetsReportDTO criticalAssets(Critical filter) {
        if (filter.minOrders() < 1) {
            throw new BusinessException("O mínimo de ordens de serviço deve ser pelo menos 1.");
        }
        PeriodRange range = PeriodRange.of(filter.from(), filter.to());

        // Manutenção por bem: OS não canceladas com bem vinculado (o custo só existe nas resolvidas).
        Map<Long, MaintenanceAccumulator> maintenance = new HashMap<>();
        for (ServiceOrder so : serviceOrderRepository.findForReport(range.from(), range.to(), null, null,
                null, null, filter.companyId())) {
            if (so.getAsset() == null || so.getStatus() == ServiceOrderStatus.CANCELLED) {
                continue;
            }
            maintenance.computeIfAbsent(so.getAsset().getId(), id -> new MaintenanceAccumulator(so.getAsset()))
                    .add(so.getCost());
        }

        Map<Long, Asset> candidates = new LinkedHashMap<>();
        assetRepository.findByConservationStates(BAD_STATES, filter.companyId())
                .forEach(a -> candidates.put(a.getId(), a));
        maintenance.forEach((id, acc) -> {
            if (acc.orders >= filter.minOrders()) {
                candidates.putIfAbsent(id, acc.asset);
            }
        });

        List<CriticalAssetRowDTO> rows = new ArrayList<>();
        for (Asset asset : candidates.values()) {
            // Bem baixado ou removido não é mais acervo: não há o que priorizar.
            if (asset.getStatus() == AssetStatus.DISCARDED || !Boolean.TRUE.equals(asset.getActive())) {
                continue;
            }
            MaintenanceAccumulator acc = maintenance.get(asset.getId());
            long orders = acc != null ? acc.orders : 0;
            List<String> reasons = new ArrayList<>();
            if (BAD_STATES.contains(asset.getConservationState())) {
                reasons.add(BAD_CONSERVATION);
            }
            if (orders >= filter.minOrders()) {
                reasons.add(RECURRENT_MAINTENANCE);
            }
            rows.add(new CriticalAssetRowDTO(asset.getId(), asset.getTagNumber(), asset.getName(),
                    asset.getSector().getName(),
                    asset.getCategory() != null ? asset.getCategory().getName() : NO_CATEGORY,
                    asset.getConservationState() != null ? asset.getConservationState().name() : null,
                    orders, acc != null ? acc.cost : BigDecimal.ZERO, reasons));
        }
        // Os que somam os dois motivos primeiro; depois mais OS, maior custo e tombamento.
        rows.sort(Comparator.comparingInt((CriticalAssetRowDTO r) -> r.reasons().size()).reversed()
                .thenComparing(Comparator.comparingLong(CriticalAssetRowDTO::orders).reversed())
                .thenComparing(CriticalAssetRowDTO::maintenanceCost, Comparator.reverseOrder())
                .thenComparing(CriticalAssetRowDTO::tagNumber));

        return new CriticalAssetsReportDTO(filter.minOrders(), rows.size(), rows);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] criticalAssetsCsv(Critical filter) {
        TableData table = criticalAssetsTable(filter);
        return CsvWriter.toBytes(table.header(), table.rows());
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] criticalAssetsPdf(Critical filter) {
        TableData table = criticalAssetsTable(filter);
        return PdfWriter.toBytes("Relatório de Bens Críticos", table.header(), table.rows(), true);
    }

    private TableData criticalAssetsTable(Critical filter) {
        List<List<String>> rows = criticalAssets(filter).assets().stream().map(a -> List.of(
                a.tagNumber(), a.name(), a.sectorName(), a.categoryName(),
                a.conservationState() != null ? conservationLabel(a.conservationState()) : "",
                String.valueOf(a.orders()), money(a.maintenanceCost()),
                String.join(" + ", a.reasons().stream().map(AssetInsightsServiceImpl::reasonLabel).toList())
        )).toList();
        return new TableData(List.of("Tombamento", "Bem", "Setor", "Categoria", "Estado de conservação",
                "Nº de OS", "Custo de manutenção", "Motivo"), rows);
    }

    private static String conservationLabel(String state) {
        return switch (AssetConservationState.valueOf(state)) {
            case NEW -> "Novo";
            case GOOD -> "Bom";
            case REGULAR -> "Regular";
            case BAD -> "Ruim";
            case UNSERVICEABLE -> "Inservível";
        };
    }

    private static String reasonLabel(String reason) {
        return BAD_CONSERVATION.equals(reason) ? "Estado ruim/inservível" : "Manutenção recorrente";
    }

    private static final class PatrimonyAccumulator {
        private final String name;
        private long assets;
        private long withoutValuation;
        private BigDecimal acquisition = BigDecimal.ZERO;
        private BigDecimal depreciation = BigDecimal.ZERO;

        private PatrimonyAccumulator(String name) {
            this.name = name;
        }
    }

    private static final class MaintenanceAccumulator {
        private final Asset asset;
        private long orders;
        private BigDecimal cost = BigDecimal.ZERO;

        private MaintenanceAccumulator(Asset asset) {
            this.asset = asset;
        }

        private void add(BigDecimal value) {
            orders++;
            if (value != null) {
                cost = cost.add(value);
            }
        }
    }
}
