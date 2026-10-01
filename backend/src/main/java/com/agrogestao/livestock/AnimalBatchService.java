package com.agrogestao.livestock;

import com.agrogestao.activities.Activity;
import com.agrogestao.activities.ActivityRepository;
import com.agrogestao.audit.AuditAction;
import com.agrogestao.audit.AuditService;
import com.agrogestao.common.exception.BusinessException;
import com.agrogestao.livestock.dto.BatchCountRequest;
import com.agrogestao.livestock.dto.BatchRequest;
import com.agrogestao.livestock.dto.BatchResponse;
import com.agrogestao.livestock.dto.HerdSummary;
import com.agrogestao.properties.TenantContext;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lotes de animais: individuais (animais com brinco) ou por contagem (só o número de cabeças). */
@Service
public class AnimalBatchService {

    private final AnimalBatchRepository batchRepository;
    private final AnimalRepository animalRepository;
    private final AnimalEventRepository eventRepository;
    private final LivestockTradeRepository tradeRepository;
    private final HealthApplicationRepository healthApplicationRepository;
    private final ActivityRepository activityRepository;
    private final LivestockSupport support;
    private final TenantContext tenantContext;
    private final AuditService auditService;

    public AnimalBatchService(
            AnimalBatchRepository batchRepository,
            AnimalRepository animalRepository,
            AnimalEventRepository eventRepository,
            LivestockTradeRepository tradeRepository,
            HealthApplicationRepository healthApplicationRepository,
            ActivityRepository activityRepository,
            LivestockSupport support,
            TenantContext tenantContext,
            AuditService auditService) {
        this.batchRepository = batchRepository;
        this.animalRepository = animalRepository;
        this.eventRepository = eventRepository;
        this.tradeRepository = tradeRepository;
        this.healthApplicationRepository = healthApplicationRepository;
        this.activityRepository = activityRepository;
        this.support = support;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    public List<BatchResponse> list(boolean includeArchived) {
        Long propertyId = tenantContext.requirePropertyId();
        List<AnimalBatch> batches = batchRepository.findByPropertyIdOrderByNameAsc(propertyId).stream()
                .filter(batch -> includeArchived || !batch.isArchived())
                .toList();
        return toResponses(propertyId, batches);
    }

    @Transactional
    public BatchResponse create(BatchRequest request) {
        tenantContext.requireWriteAccess();
        Long propertyId = tenantContext.requirePropertyId();
        String name = request.name().trim();
        if (batchRepository.existsByPropertyIdAndNameIgnoreCase(propertyId, name)) {
            throw new BusinessException("Já existe um lote chamado " + name + ".");
        }
        validate(propertyId, request);
        AnimalBatch batch = new AnimalBatch();
        batch.setPropertyId(propertyId);
        batch.setName(name);
        batch.setSpeciesId(support.ensureSpeciesId(propertyId, request.species()));
        batch.setTracking(request.tracking());
        batch.setActivityId(request.activityId());
        batch.setNotes(blankToNull(request.notes()));
        batchRepository.save(batch);
        auditService.record("AnimalBatch", batch.getId(), AuditAction.CREATE, describe(batch, request.species()));
        return toResponses(propertyId, List.of(batch)).get(0);
    }

    @Transactional
    public BatchResponse update(Long id, BatchRequest request) {
        tenantContext.requireWriteAccess();
        Long propertyId = tenantContext.requirePropertyId();
        AnimalBatch batch = support.findBatch(propertyId, id);
        String name = request.name().trim();
        if (batchRepository.existsByPropertyIdAndNameIgnoreCaseAndIdNot(propertyId, name, id)) {
            throw new BusinessException("Já existe um lote chamado " + name + ".");
        }
        validate(propertyId, request);
        boolean structureChanged = batch.getSpeciesId() != support.findSpeciesId(propertyId, request.species())
                || batch.getTracking() != request.tracking();
        if (structureChanged && hasHistory(batch)) {
            throw new BusinessException("A espécie e o tipo de controle não mudam depois que o lote tem animais "
                    + "ou movimentações. Crie um lote novo.");
        }
        batch.setName(name);
        batch.setSpeciesId(support.ensureSpeciesId(propertyId, request.species()));
        batch.setTracking(request.tracking());
        batch.setActivityId(request.activityId());
        batch.setNotes(blankToNull(request.notes()));
        batchRepository.save(batch);
        auditService.record("AnimalBatch", batch.getId(), AuditAction.UPDATE, describe(batch, request.species()));
        return toResponses(propertyId, List.of(batch)).get(0);
    }

    /** Arquiva um lote vazio: some das listas e formulários, mas fica no histórico. */
    @Transactional
    public BatchResponse setArchived(Long id, boolean archived) {
        tenantContext.requireManageAccess();
        Long propertyId = tenantContext.requirePropertyId();
        AnimalBatch batch = support.findBatch(propertyId, id);
        if (archived && support.headCounts(propertyId, List.of(batch)).get(batch.getId()) > 0) {
            throw new BusinessException("O lote " + batch.getName()
                    + " ainda tem animais. Mova-os ou registre a saída antes de arquivar.");
        }
        batch.setArchivedAt(archived ? LocalDateTime.now() : null);
        batchRepository.save(batch);
        auditService.record("AnimalBatch", batch.getId(), AuditAction.UPDATE,
                (archived ? "arquivado: " : "reativado: ") + batch.getName());
        return toResponses(propertyId, List.of(batch)).get(0);
    }

    /** Exclui um lote criado por engano (sem animais nem movimentações). Com histórico, arquive. */
    @Transactional
    public void delete(Long id) {
        tenantContext.requireManageAccess();
        AnimalBatch batch = support.findBatch(tenantContext.requirePropertyId(), id);
        if (hasHistory(batch)) {
            throw new BusinessException("O lote " + batch.getName()
                    + " tem animais ou movimentações no histórico. Arquive-o em vez de excluir.");
        }
        batchRepository.delete(batch);
        auditService.record("AnimalBatch", batch.getId(), AuditAction.DELETE, batch.getName());
    }

    /** Entrada de cabeças num lote por contagem sem compra (nascimento, eclosão, transferência...). */
    @Transactional
    public BatchResponse addHeads(Long id, BatchCountRequest request) {
        tenantContext.requireWriteAccess();
        Long propertyId = tenantContext.requirePropertyId();
        AnimalBatch batch = support.requireAggregateBatch(propertyId, id);
        LivestockSupport.requireNotFuture(request.date());
        batch.setHeadCount(batch.getHeadCount() + request.headCount());
        batchRepository.save(batch);
        String description = blankToNull(request.description()) != null ? request.description().trim() : "Entrada";
        support.saveEvent(propertyId, AnimalEventType.ENTRADA, request.date(), null, batch, request.headCount(),
                null, null, null, description);
        auditService.record("AnimalBatch", batch.getId(), AuditAction.UPDATE, "%s: +%s (%s)".formatted(batch.getName(),
                LivestockSupport.plural(request.headCount(), "cabeça", "cabeças"), description));
        return toResponses(propertyId, List.of(batch)).get(0);
    }

    /** Saída de cabeças sem venda (morte, descarte...). A venda é feita pela venda de animais. */
    @Transactional
    public BatchResponse removeHeads(Long id, BatchCountRequest request) {
        tenantContext.requireWriteAccess();
        Long propertyId = tenantContext.requirePropertyId();
        AnimalBatch batch = support.requireAggregateBatch(propertyId, id);
        LivestockSupport.requireNotFuture(request.date());
        if (request.exitType() == null) {
            throw new BusinessException("Informe o motivo da saída (morte, descarte ou outra).");
        }
        if (request.headCount() > batch.getHeadCount()) {
            throw new BusinessException("O lote " + batch.getName() + " tem só "
                    + LivestockSupport.plural(batch.getHeadCount(), "cabeça", "cabeças") + ".");
        }
        batch.setHeadCount(batch.getHeadCount() - request.headCount());
        batchRepository.save(batch);
        String description = request.exitType().label()
                + (blankToNull(request.description()) != null ? ": " + request.description().trim() : "");
        support.saveEvent(propertyId, AnimalEventType.SAIDA, request.date(), null, batch, request.headCount(),
                null, null, null, description);
        auditService.record("AnimalBatch", batch.getId(), AuditAction.UPDATE, "%s: −%s (%s)".formatted(batch.getName(),
                LivestockSupport.plural(request.headCount(), "cabeça", "cabeças"), description));
        return toResponses(propertyId, List.of(batch)).get(0);
    }

    /** Cabeças ativas por espécie: animais com brinco + lotes por contagem não arquivados. */
    public HerdSummary summary() {
        Long propertyId = tenantContext.requirePropertyId();
        Map<Long, Long> headsBySpeciesId = new java.util.HashMap<>();
        animalRepository.countActiveBySpecies(propertyId)
                .forEach(row -> headsBySpeciesId.merge((Long) row[0], (Long) row[1], Long::sum));
        batchRepository.findByPropertyIdOrderByNameAsc(propertyId).stream()
                .filter(batch -> batch.isAggregate() && !batch.isArchived() && batch.getHeadCount() > 0)
                .forEach(batch -> headsBySpeciesId.merge(batch.getSpeciesId(), (long) batch.getHeadCount(), Long::sum));
        Map<Long, Species> species = support.speciesOf(headsBySpeciesId.keySet());
        List<HerdSummary.SpeciesCount> bySpecies = headsBySpeciesId.entrySet().stream()
                .map(entry -> new HerdSummary.SpeciesCount(species.get(entry.getKey()), entry.getValue()))
                .sorted(Comparator.comparingLong(HerdSummary.SpeciesCount::heads).reversed())
                .toList();
        return new HerdSummary(bySpecies.stream().mapToLong(HerdSummary.SpeciesCount::heads).sum(), bySpecies);
    }

    private void validate(Long propertyId, BatchRequest request) {
        if (request.tracking() == BatchTracking.INDIVIDUAL) {
            LivestockSupport.requireIndividualSpecies(request.species());
        }
        if (request.activityId() != null && !activityRepository.existsByIdAndPropertyId(request.activityId(), propertyId)) {
            throw new BusinessException("Atividade inválida para esta propriedade");
        }
    }

    private boolean hasHistory(AnimalBatch batch) {
        return batch.getHeadCount() > 0
                || animalRepository.existsByBatchId(batch.getId())
                || eventRepository.existsByBatchIdOrFromBatchIdOrToBatchId(batch.getId(), batch.getId(), batch.getId())
                || tradeRepository.existsByBatchId(batch.getId())
                || healthApplicationRepository.existsByBatchId(batch.getId());
    }

    private List<BatchResponse> toResponses(Long propertyId, List<AnimalBatch> batches) {
        Map<Long, Integer> heads = support.headCounts(propertyId, batches);
        Map<Long, String> activities = activityRepository.findByPropertyId(propertyId).stream()
                .collect(Collectors.toMap(Activity::getId, Activity::getName));
        Map<Long, java.time.LocalDate> withdrawals = new java.util.HashMap<>();
        eventRepository.batchWithdrawals(propertyId, java.time.LocalDate.now())
                .forEach(row -> withdrawals.put((Long) row[0], (java.time.LocalDate) row[1]));
        Map<Long, Species> species = support.speciesOf(batches.stream().map(AnimalBatch::getSpeciesId).toList());
        return batches.stream().map(b -> new BatchResponse(b.getId(), b.getName(), species.get(b.getSpeciesId()),
                b.getTracking(), heads.get(b.getId()), b.getActivityId(),
                b.getActivityId() != null ? activities.get(b.getActivityId()) : null,
                b.getNotes(), b.isArchived(), withdrawals.get(b.getId()))).toList();
    }

    private static String describe(AnimalBatch batch, Species species) {
        return "%s (%s, %s)".formatted(batch.getName(), species.label().toLowerCase(),
                batch.isAggregate() ? "por contagem" : "animais com brinco");
    }

    static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
