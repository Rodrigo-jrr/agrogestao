package com.agrogestao.livestock;

import com.agrogestao.finance.CostCenter;
import com.agrogestao.finance.CostCenterRepository;
import com.agrogestao.finance.FinancialTransaction;
import com.agrogestao.finance.FinancialTransactionRepository;
import com.agrogestao.livestock.dto.LivestockIndicators;
import com.agrogestao.properties.TenantContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Indicadores da pecuária: reprodução (prenhez, natalidade, mortalidade, intervalo entre partos), GMD e
 * custo por animal/kg, por lote ou de toda a propriedade, num período. Calcula em memória sobre os eventos
 * já registrados (entrada, sanidade, pesagem, saída, reprodução) — sem tabela própria, como os demais
 * indicadores do sistema.
 */
@Service
public class LivestockIndicatorsService {

    private static final LocalDate EARLIEST = LocalDate.of(1900, 1, 1);

    private final AnimalRepository animalRepository;
    private final AnimalBatchRepository batchRepository;
    private final AnimalEventRepository eventRepository;
    private final ReproductiveEventRepository reproductiveEventRepository;
    private final CostCenterRepository costCenterRepository;
    private final FinancialTransactionRepository financialTransactionRepository;
    private final LivestockTradeRepository tradeRepository;
    private final LivestockSupport support;
    private final TenantContext tenantContext;

    public LivestockIndicatorsService(
            AnimalRepository animalRepository,
            AnimalBatchRepository batchRepository,
            AnimalEventRepository eventRepository,
            ReproductiveEventRepository reproductiveEventRepository,
            CostCenterRepository costCenterRepository,
            FinancialTransactionRepository financialTransactionRepository,
            LivestockTradeRepository tradeRepository,
            LivestockSupport support,
            TenantContext tenantContext) {
        this.animalRepository = animalRepository;
        this.batchRepository = batchRepository;
        this.eventRepository = eventRepository;
        this.reproductiveEventRepository = reproductiveEventRepository;
        this.costCenterRepository = costCenterRepository;
        this.financialTransactionRepository = financialTransactionRepository;
        this.tradeRepository = tradeRepository;
        this.support = support;
        this.tenantContext = tenantContext;
    }

    public LivestockIndicators get(Long batchId, LocalDate from, LocalDate to) {
        Long propertyId = tenantContext.requirePropertyId();
        LocalDate periodFrom = from != null ? from : EARLIEST;
        LocalDate periodTo = to != null ? to : LocalDate.now();

        AnimalBatch scopeBatch = batchId != null ? support.findBatch(propertyId, batchId) : null;
        List<AnimalBatch> batches = scopeBatch != null ? List.of(scopeBatch)
                : batchRepository.findByPropertyIdOrderByNameAsc(propertyId);
        List<Animal> animals = batchId != null ? animalRepository.findByPropertyIdAndBatchId(propertyId, batchId)
                : animalRepository.findByPropertyId(propertyId);
        Set<Long> animalIds = animals.stream().map(Animal::getId).collect(Collectors.toSet());
        Set<Long> batchIds = batches.stream().map(AnimalBatch::getId).collect(Collectors.toSet());
        Set<Long> aggregateBatchIds = batches.stream().filter(AnimalBatch::isAggregate)
                .map(AnimalBatch::getId).collect(Collectors.toSet());

        long headCount = headCount(scopeBatch, batches, animals);
        BigDecimal liveWeight = liveWeight(scopeBatch, batches, animals, periodTo);

        ReproductionCounts reproduction = reproductionCounts(propertyId, animals, periodFrom, periodTo);
        int deaths = deaths(animals, aggregateBatchIds, propertyId, periodFrom, periodTo);
        BigDecimal averageDailyGain = averageDailyGain(animalIds, aggregateBatchIds, propertyId, periodFrom, periodTo);
        Costs costs = costs(propertyId, animalIds, batchIds, batches, periodFrom, periodTo);

        BigDecimal pregnancyRate = percent(reproduction.positiveDiagnoses, reproduction.diagnoses);
        long activeFemales = animals.stream().filter(a -> a.getSex() == Sex.FEMEA && a.isActive()).count();
        BigDecimal birthRate = percent(reproduction.offspring, (int) activeFemales);
        BigDecimal mortalityRate = percent(deaths, (int) (headCount + deaths));
        BigDecimal costPerAnimal = headCount == 0 ? null
                : costs.total().divide(BigDecimal.valueOf(headCount), 2, RoundingMode.HALF_UP);
        BigDecimal costPerKg = liveWeight.compareTo(BigDecimal.ZERO) <= 0 ? null
                : costs.total().divide(liveWeight, 2, RoundingMode.HALF_UP);

        return new LivestockIndicators(batchId, scopeBatch != null ? scopeBatch.getName() : null, from, to, headCount,
                reproduction.diagnoses, reproduction.positiveDiagnoses, pregnancyRate,
                reproduction.offspring, birthRate,
                deaths, mortalityRate,
                reproduction.averageCalvingIntervalDays,
                averageDailyGain,
                costs.purchase(), costs.health(), costs.general(), costs.total(),
                costPerAnimal, costPerKg);
    }

    private long headCount(AnimalBatch scopeBatch, List<AnimalBatch> batches, List<Animal> animals) {
        long activeIndividual = animals.stream().filter(Animal::isActive).count();
        if (scopeBatch != null) {
            return scopeBatch.isAggregate() ? scopeBatch.getHeadCount() : activeIndividual;
        }
        long aggregateHeads = batches.stream().filter(AnimalBatch::isAggregate).filter(b -> !b.isArchived())
                .mapToLong(AnimalBatch::getHeadCount).sum();
        return activeIndividual + aggregateHeads;
    }

    private BigDecimal liveWeight(AnimalBatch scopeBatch, List<AnimalBatch> batches, List<Animal> animals, LocalDate asOf) {
        BigDecimal individual = animals.stream().filter(Animal::isActive).map(Animal::getCurrentWeight)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (scopeBatch != null) {
            return scopeBatch.isAggregate() ? aggregateWeight(scopeBatch, asOf) : individual;
        }
        BigDecimal aggregate = batches.stream().filter(AnimalBatch::isAggregate).filter(b -> !b.isArchived())
                .map(b -> aggregateWeight(b, asOf)).reduce(BigDecimal.ZERO, BigDecimal::add);
        return individual.add(aggregate);
    }

    /** Peso médio × cabeças de um lote por contagem, pela pesagem mais recente até {@code asOf}. */
    private BigDecimal aggregateWeight(AnimalBatch batch, LocalDate asOf) {
        return eventRepository.findByBatchIdOrderByEventDateDescIdDesc(batch.getId()).stream()
                .filter(e -> e.getType() == AnimalEventType.PESAGEM && e.getWeight() != null && !e.getEventDate().isAfter(asOf))
                .findFirst()
                .map(e -> e.getWeight().multiply(BigDecimal.valueOf(batch.getHeadCount())))
                .orElse(BigDecimal.ZERO);
    }

    private record ReproductionCounts(int diagnoses, int positiveDiagnoses, int offspring, BigDecimal averageCalvingIntervalDays) {
    }

    private ReproductionCounts reproductionCounts(Long propertyId, List<Animal> animals, LocalDate from, LocalDate to) {
        Set<Long> femaleIds = animals.stream().filter(a -> a.getSex() == Sex.FEMEA).map(Animal::getId)
                .collect(Collectors.toSet());
        if (femaleIds.isEmpty()) {
            return new ReproductionCounts(0, 0, 0, null);
        }
        List<ReproductiveEvent> events = reproductiveEventRepository
                .findByPropertyIdOrderByAnimalIdAscEventDateDescIdDesc(propertyId).stream()
                .filter(e -> femaleIds.contains(e.getAnimalId()))
                .toList();

        List<ReproductiveEvent> diagnosesInPeriod = events.stream()
                .filter(e -> e.getKind() == ReproductiveEventKind.DIAGNOSTICO_PRENHEZ && inPeriod(e.getEventDate(), from, to))
                .toList();
        int positive = (int) diagnosesInPeriod.stream().filter(e -> e.getPregnancyResult() == PregnancyResult.POSITIVO).count();

        List<ReproductiveEvent> birthsInPeriod = events.stream()
                .filter(e -> e.getKind() == ReproductiveEventKind.PARTO && inPeriod(e.getEventDate(), from, to))
                .toList();
        int offspring = birthsInPeriod.stream().mapToInt(e -> e.getOffspringCount() != null ? e.getOffspringCount() : 0).sum();

        Map<Long, List<ReproductiveEvent>> birthsByFemale = events.stream()
                .filter(e -> e.getKind() == ReproductiveEventKind.PARTO)
                .collect(Collectors.groupingBy(ReproductiveEvent::getAnimalId));
        List<Long> intervalDays = new ArrayList<>();
        for (List<ReproductiveEvent> births : birthsByFemale.values()) {
            List<ReproductiveEvent> sorted = births.stream()
                    .sorted(Comparator.comparing(ReproductiveEvent::getEventDate)).toList();
            for (int i = 1; i < sorted.size(); i++) {
                LocalDate previous = sorted.get(i - 1).getEventDate();
                LocalDate current = sorted.get(i).getEventDate();
                if (inPeriod(current, from, to)) {
                    intervalDays.add(ChronoUnit.DAYS.between(previous, current));
                }
            }
        }
        BigDecimal averageInterval = intervalDays.isEmpty() ? null
                : BigDecimal.valueOf(intervalDays.stream().mapToLong(Long::longValue).average().orElse(0))
                        .setScale(1, RoundingMode.HALF_UP);
        return new ReproductionCounts(diagnosesInPeriod.size(), positive, offspring, averageInterval);
    }

    private int deaths(List<Animal> animals, Set<Long> aggregateBatchIds, Long propertyId, LocalDate from, LocalDate to) {
        int individual = (int) animals.stream()
                .filter(a -> a.getStatus() == AnimalStatus.MORTO && a.getExitDate() != null && inPeriod(a.getExitDate(), from, to))
                .count();
        if (aggregateBatchIds.isEmpty()) {
            return individual;
        }
        int aggregate = eventRepository.findByPropertyIdAndTypeAndEventDateBetween(propertyId, AnimalEventType.SAIDA, from, to)
                .stream()
                .filter(e -> e.getBatchId() != null && aggregateBatchIds.contains(e.getBatchId())
                        && e.getDescription() != null && e.getDescription().startsWith(ExitType.MORTE.label()))
                .mapToInt(e -> e.getHeadCount() != null ? e.getHeadCount() : 0)
                .sum();
        return individual + aggregate;
    }

    /** Ganho médio diário: (peso da última pesagem − peso da primeira) / dias, por animal e por lote por contagem, com a média. */
    private BigDecimal averageDailyGain(Set<Long> animalIds, Set<Long> aggregateBatchIds, Long propertyId,
            LocalDate from, LocalDate to) {
        List<AnimalEvent> weighings = eventRepository
                .findByPropertyIdAndTypeAndEventDateBetween(propertyId, AnimalEventType.PESAGEM, from, to);
        List<BigDecimal> samples = new ArrayList<>();
        Map<Long, List<AnimalEvent>> byAnimal = weighings.stream()
                .filter(e -> e.getAnimalId() != null && animalIds.contains(e.getAnimalId()))
                .collect(Collectors.groupingBy(AnimalEvent::getAnimalId));
        byAnimal.values().forEach(events -> addGainSample(events, samples));
        Map<Long, List<AnimalEvent>> byBatch = weighings.stream()
                .filter(e -> e.getBatchId() != null && aggregateBatchIds.contains(e.getBatchId()))
                .collect(Collectors.groupingBy(AnimalEvent::getBatchId));
        byBatch.values().forEach(events -> addGainSample(events, samples));
        if (samples.isEmpty()) {
            return null;
        }
        BigDecimal sum = samples.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(samples.size()), 3, RoundingMode.HALF_UP);
    }

    private static void addGainSample(List<AnimalEvent> events, List<BigDecimal> samples) {
        List<AnimalEvent> sorted = events.stream()
                .filter(e -> e.getWeight() != null)
                .sorted(Comparator.comparing(AnimalEvent::getEventDate)).toList();
        if (sorted.size() < 2) {
            return;
        }
        AnimalEvent first = sorted.get(0);
        AnimalEvent last = sorted.get(sorted.size() - 1);
        long days = ChronoUnit.DAYS.between(first.getEventDate(), last.getEventDate());
        if (days <= 0) {
            return;
        }
        BigDecimal gain = last.getWeight().subtract(first.getWeight());
        samples.add(gain.divide(BigDecimal.valueOf(days), 3, RoundingMode.HALF_UP));
    }

    private record Costs(BigDecimal purchase, BigDecimal health, BigDecimal general) {
        BigDecimal total() {
            return purchase.add(health).add(general);
        }
    }

    private Costs costs(Long propertyId, Set<Long> animalIds, Set<Long> batchIds, List<AnimalBatch> batches,
            LocalDate from, LocalDate to) {
        BigDecimal purchase = eventRepository.findByPropertyIdAndTypeAndEventDateBetween(propertyId, AnimalEventType.ENTRADA, from, to)
                .stream()
                .filter(e -> e.getTradeId() != null && inScope(e, animalIds, batchIds))
                .map(e -> e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal health = eventRepository.findByPropertyIdAndTypeAndEventDateBetween(propertyId, AnimalEventType.SANIDADE, from, to)
                .stream()
                .filter(e -> inScope(e, animalIds, batchIds))
                .map(e -> e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal general = generalCosts(propertyId, batches, from, to);
        return new Costs(purchase, health, general);
    }

    private static boolean inScope(AnimalEvent e, Set<Long> animalIds, Set<Long> batchIds) {
        if (e.getAnimalId() != null) {
            return animalIds.contains(e.getAnimalId());
        }
        return e.getBatchId() != null && batchIds.contains(e.getBatchId());
    }

    /**
     * Despesas gerais (ração, mão de obra etc.) do(s) centro(s) de custo da(s) atividade(s) dos lotes no escopo,
     * no período — exclui lançamentos já gerados por compra/venda de animais (contados em {@code purchase}).
     */
    private BigDecimal generalCosts(Long propertyId, List<AnimalBatch> batches, LocalDate from, LocalDate to) {
        Set<Long> activityIds = batches.stream().map(AnimalBatch::getActivityId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (activityIds.isEmpty()) {
            return BigDecimal.ZERO;
        }
        List<Long> costCenterIds = costCenterRepository.findByPropertyId(propertyId).stream()
                .filter(cc -> cc.getActivityId() != null && activityIds.contains(cc.getActivityId()))
                .map(CostCenter::getId).toList();
        if (costCenterIds.isEmpty()) {
            return BigDecimal.ZERO;
        }
        Set<Long> tradeTransactionIds = new HashSet<>(tradeRepository.findFinancialTransactionIds(propertyId));
        return financialTransactionRepository.findExpensesByCostCenters(propertyId, costCenterIds, from, to).stream()
                .filter(t -> !tradeTransactionIds.contains(t.getId()))
                .map(FinancialTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static boolean inPeriod(LocalDate date, LocalDate from, LocalDate to) {
        return !date.isBefore(from) && !date.isAfter(to);
    }

    private static BigDecimal percent(int numerator, int denominator) {
        if (denominator <= 0) {
            return null;
        }
        return BigDecimal.valueOf(numerator).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }
}
