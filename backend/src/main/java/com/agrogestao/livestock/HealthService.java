package com.agrogestao.livestock;

import com.agrogestao.audit.AuditAction;
import com.agrogestao.audit.AuditService;
import com.agrogestao.common.Money;
import com.agrogestao.common.PageResponse;
import com.agrogestao.common.exception.BusinessException;
import com.agrogestao.common.exception.ResourceNotFoundException;
import com.agrogestao.inventory.InventoryMovement;
import com.agrogestao.inventory.InventoryMovementRepository;
import com.agrogestao.inventory.InventoryMovementService;
import com.agrogestao.inventory.MovementType;
import com.agrogestao.inventory.Product;
import com.agrogestao.livestock.dto.HealthApplicationRequest;
import com.agrogestao.livestock.dto.HealthApplicationResponse;
import com.agrogestao.livestock.dto.LivestockAlerts;
import com.agrogestao.properties.TenantContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manejo sanitário: vacinas, medicamentos e procedimentos em animais ou lotes. Produto do estoque sai pela
 * dose × cabeças, ao custo médio, rateado entre os animais. Cada aplicação pode deixar carência (o animal
 * não deve ir para abate/ordenha comercial até a data) e um próximo passo (reforço, fim do tratamento),
 * que aparece nos alertas até ser registrado ou dispensado.
 */
@Service
public class HealthService {

    /** Alertas olham os próximos 7 dias (e tudo o que está atrasado). */
    static final int ALERT_WINDOW_DAYS = 7;

    private final HealthApplicationRepository applicationRepository;
    private final AnimalRepository animalRepository;
    private final AnimalEventRepository eventRepository;
    private final InventoryMovementService inventoryMovementService;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final LivestockSupport support;
    private final TenantContext tenantContext;
    private final AuditService auditService;

    public HealthService(
            HealthApplicationRepository applicationRepository,
            AnimalRepository animalRepository,
            AnimalEventRepository eventRepository,
            InventoryMovementService inventoryMovementService,
            InventoryMovementRepository inventoryMovementRepository,
            LivestockSupport support,
            TenantContext tenantContext,
            AuditService auditService) {
        this.applicationRepository = applicationRepository;
        this.animalRepository = animalRepository;
        this.eventRepository = eventRepository;
        this.inventoryMovementService = inventoryMovementService;
        this.inventoryMovementRepository = inventoryMovementRepository;
        this.support = support;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    public PageResponse<HealthApplicationResponse> list(Pageable pageable) {
        Long propertyId = tenantContext.requirePropertyId();
        Page<HealthApplication> page = applicationRepository.findByPropertyIdOrderByApplicationDateDescIdDesc(propertyId, pageable);
        return new PageResponse<>(toResponses(propertyId, page.getContent()), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    @Transactional
    public HealthApplicationResponse apply(HealthApplicationRequest request) {
        tenantContext.requireWriteAccess();
        Long propertyId = tenantContext.requirePropertyId();
        LivestockSupport.requireNotFuture(request.date());
        boolean byAnimals = request.animalIds() != null && !request.animalIds().isEmpty();
        if (byAnimals == (request.batchId() != null)) {
            throw new BusinessException("Escolha os animais ou o lote que recebeu a aplicação.");
        }
        if (request.nextDate() != null && request.nextDate().isBefore(request.date())) {
            throw new BusinessException("A próxima data não pode ser anterior à aplicação.");
        }

        // Alvo: animais com brinco (escolhidos ou todos os ativos de um lote individual) ou um lote por contagem.
        AnimalBatch batch = request.batchId() != null ? support.requireBatch(propertyId, request.batchId()) : null;
        List<Animal> animals;
        int heads;
        if (byAnimals) {
            List<Long> ids = List.copyOf(new LinkedHashSet<>(request.animalIds()));
            animals = animalRepository.findByPropertyIdAndIdIn(propertyId, ids);
            if (animals.size() != ids.size()) {
                throw new BusinessException("Algum animal selecionado não pertence a esta propriedade.");
            }
            heads = animals.size();
        } else if (batch.isAggregate()) {
            animals = List.of();
            heads = batch.getHeadCount();
            if (heads == 0) {
                throw new BusinessException("O lote " + batch.getName() + " está sem animais.");
            }
        } else {
            animals = animalRepository.search(propertyId, AnimalStatus.ATIVO, batch.getId(), null, null,
                    Pageable.unpaged()).getContent();
            heads = animals.size();
            if (heads == 0) {
                throw new BusinessException("O lote " + batch.getName() + " está sem animais ativos.");
            }
        }
        for (Animal animal : animals) {
            if (!animal.isActive()) {
                throw new BusinessException("O animal " + animal.getTag() + " não está no rebanho.");
            }
            if (request.date().isBefore(animal.getEntryDate())) {
                throw new BusinessException("A aplicação é anterior à entrada do animal " + animal.getTag() + ".");
            }
        }

        HealthApplication application = new HealthApplication();
        application.setPropertyId(propertyId);
        application.setKind(request.kind());
        application.setApplicationDate(request.date());
        application.setBatchId(batch != null ? batch.getId() : null);
        application.setHeadCount(heads);
        application.setWithdrawalDays(request.withdrawalDays() != null && request.withdrawalDays() > 0 ? request.withdrawalDays() : null);
        application.setWithdrawalUntil(application.getWithdrawalDays() != null
                ? request.date().plusDays(application.getWithdrawalDays()) : null);
        application.setNextDate(request.nextDate());
        application.setNextDescription(AnimalBatchService.blankToNull(request.nextDescription()));
        application.setResponsible(AnimalBatchService.blankToNull(request.responsible()));
        application.setNotes(AnimalBatchService.blankToNull(request.notes()));

        if (request.productId() != null) {
            Product product = inventoryMovementService.requireActiveProduct(propertyId, request.productId());
            if (request.dose() == null) {
                throw new BusinessException("Informe a dose por cabeça (em " + product.getUnit() + ").");
            }
            BigDecimal total = request.dose().multiply(BigDecimal.valueOf(heads)).setScale(3, RoundingMode.HALF_UP);
            InventoryMovement movement = inventoryMovementService.registerMovement(propertyId, product.getId(),
                    MovementType.OUT, total, null, request.date(),
                    request.kind().label() + " — " + describeTarget(batch, animals, heads));
            BigDecimal unitCost = movement.getUnitCost() != null ? movement.getUnitCost() : BigDecimal.ZERO;
            application.setProductId(product.getId());
            application.setProductName(product.getName());
            application.setDose(request.dose());
            application.setDoseUnit(product.getUnit());
            application.setTotalQuantity(total);
            application.setTotalCost(unitCost.multiply(total).setScale(2, RoundingMode.HALF_UP));
            application.setInventoryMovementId(movement.getId());
        } else {
            String name = AnimalBatchService.blankToNull(request.productName());
            if (name == null) {
                throw new BusinessException("Escolha um produto do estoque ou informe o nome do que foi aplicado.");
            }
            application.setProductName(name);
            application.setDose(request.dose());
            application.setDoseUnit(AnimalBatchService.blankToNull(request.doseUnit()));
            application.setTotalQuantity(request.dose() != null
                    ? request.dose().multiply(BigDecimal.valueOf(heads)).setScale(3, RoundingMode.HALF_UP) : null);
            application.setTotalCost(request.cost() != null ? request.cost().setScale(2, RoundingMode.HALF_UP) : null);
        }
        applicationRepository.save(application);

        String description = describe(application);
        if (animals.isEmpty()) {
            AnimalEvent event = support.saveEvent(propertyId, AnimalEventType.SANIDADE, request.date(), null, batch,
                    heads, null, null, application.getTotalCost(), description);
            link(event, application);
        } else {
            List<BigDecimal> shares = application.getTotalCost() != null
                    ? LivestockSupport.splitAmount(application.getTotalCost(), heads) : null;
            for (int i = 0; i < animals.size(); i++) {
                AnimalEvent event = support.saveEvent(propertyId, AnimalEventType.SANIDADE, request.date(),
                        animals.get(i), null, null, null, null, shares != null ? shares.get(i) : null, description);
                link(event, application);
            }
        }

        if (request.followUpOfId() != null) {
            applicationRepository.findById(request.followUpOfId())
                    .filter(previous -> previous.getPropertyId().equals(propertyId))
                    .ifPresent(previous -> {
                        previous.setFollowUpDoneAt(LocalDateTime.now());
                        applicationRepository.save(previous);
                    });
        }
        auditService.record("HealthApplication", application.getId(), AuditAction.CREATE,
                description + " — " + describeTarget(batch, animals, heads)
                        + (application.getTotalCost() != null ? " (" + Money.brl(application.getTotalCost()) + ")" : ""));
        return toResponses(propertyId, List.of(application)).get(0);
    }

    /** Desfaz uma aplicação lançada errada: some da linha do tempo e o produto volta ao estoque. */
    @Transactional
    public void undo(Long id) {
        tenantContext.requireManageAccess();
        Long propertyId = tenantContext.requirePropertyId();
        HealthApplication application = findOwned(propertyId, id);
        eventRepository.deleteAll(eventRepository.findByApplicationId(id));
        if (application.getInventoryMovementId() != null) {
            InventoryMovement out = inventoryMovementRepository.findById(application.getInventoryMovementId()).orElseThrow();
            inventoryMovementService.registerMovement(propertyId, out.getProductId(), MovementType.IN, out.getQuantity(),
                    out.getUnitCost(), LocalDate.now(), "Aplicação desfeita: " + application.getProductName());
        }
        applicationRepository.delete(application);
        auditService.record("HealthApplication", id, AuditAction.DELETE, "aplicação desfeita: " + describe(application));
    }

    /** Marca o próximo passo (reforço, fim do tratamento) como resolvido, sem registrar nova aplicação. */
    @Transactional
    public HealthApplicationResponse dismissFollowUp(Long id) {
        tenantContext.requireWriteAccess();
        Long propertyId = tenantContext.requirePropertyId();
        HealthApplication application = findOwned(propertyId, id);
        application.setFollowUpDoneAt(LocalDateTime.now());
        applicationRepository.save(application);
        auditService.record("HealthApplication", id, AuditAction.UPDATE, "próximo passo dispensado: " + describe(application));
        return toResponses(propertyId, List.of(application)).get(0);
    }

    /** Reforços e fins de tratamento pendentes nos próximos {@link #ALERT_WINDOW_DAYS} dias (e os atrasados). */
    public List<HealthApplicationResponse> pendingFollowUps() {
        Long propertyId = tenantContext.requirePropertyId();
        return toResponses(propertyId,
                applicationRepository.findPendingFollowUps(propertyId, LocalDate.now().plusDays(ALERT_WINDOW_DAYS)));
    }

    /** Animais e lotes por contagem ainda em carência hoje. */
    public List<LivestockAlerts.Withdrawal> withdrawalAlerts() {
        Long propertyId = tenantContext.requirePropertyId();
        LocalDate today = LocalDate.now();
        List<LivestockAlerts.Withdrawal> withdrawals = new ArrayList<>();
        List<Object[]> animalRows = eventRepository.activeWithdrawals(propertyId, today);
        Map<Long, String> tags = animalRepository.findAllById(animalRows.stream().map(r -> (Long) r[0]).toList()).stream()
                .collect(Collectors.toMap(Animal::getId, Animal::getTag));
        animalRows.forEach(row -> withdrawals.add(new LivestockAlerts.Withdrawal((Long) row[0], null,
                "Animal " + tags.get((Long) row[0]), (LocalDate) row[1])));
        Map<Long, AnimalBatch> batches = support.batchesOf(propertyId);
        eventRepository.batchWithdrawals(propertyId, today).forEach(row -> {
            AnimalBatch batch = batches.get((Long) row[0]);
            if (batch != null && batch.getHeadCount() > 0) {
                withdrawals.add(new LivestockAlerts.Withdrawal(null, batch.getId(), "Lote " + batch.getName(), (LocalDate) row[1]));
            }
        });
        withdrawals.sort(Comparator.comparing(LivestockAlerts.Withdrawal::until).thenComparing(LivestockAlerts.Withdrawal::label));
        return withdrawals;
    }

    private static void link(AnimalEvent event, HealthApplication application) {
        event.setApplicationId(application.getId());
        event.setWithdrawalUntil(application.getWithdrawalUntil());
    }

    private HealthApplication findOwned(Long propertyId, Long id) {
        return applicationRepository.findById(id)
                .filter(a -> a.getPropertyId().equals(propertyId))
                .orElseThrow(() -> new ResourceNotFoundException("Aplicação não encontrada"));
    }

    /** "Vacina: Aftosa (5 mL)" */
    private static String describe(HealthApplication a) {
        String dose = a.getDose() != null
                ? " (" + a.getDose().stripTrailingZeros().toPlainString() + (a.getDoseUnit() != null ? " " + a.getDoseUnit() : "") + ")"
                : "";
        // "Vacina Aftosa" já diz o tipo: evita "Vacina: Vacina Aftosa".
        boolean named = a.getProductName().toLowerCase(java.util.Locale.ROOT)
                .startsWith(a.getKind().label().toLowerCase(java.util.Locale.ROOT));
        return (named ? "" : a.getKind().label() + ": ") + a.getProductName() + dose;
    }

    private static String describeTarget(AnimalBatch batch, List<Animal> animals, int heads) {
        if (batch != null) {
            return "lote " + batch.getName();
        }
        return animals.size() == 1 ? "animal " + animals.get(0).getTag() : LivestockSupport.plural(heads, "animal", "animais");
    }

    private List<HealthApplicationResponse> toResponses(Long propertyId, List<HealthApplication> applications) {
        Map<Long, AnimalBatch> batches = support.batchesOf(propertyId);
        return applications.stream().map(a -> {
            List<AnimalEvent> events = eventRepository.findByApplicationId(a.getId());
            List<Long> animalIds = events.stream().map(AnimalEvent::getAnimalId).filter(Objects::nonNull).toList();
            List<HealthApplicationResponse.AnimalRef> refs = animalRepository.findAllById(animalIds).stream()
                    .sorted(Comparator.comparing(Animal::getTag))
                    .map(animal -> new HealthApplicationResponse.AnimalRef(animal.getId(), animal.getTag()))
                    .toList();
            AnimalBatch batch = a.getBatchId() != null ? batches.get(a.getBatchId()) : null;
            String target = batch != null ? "lote " + batch.getName()
                    : refs.size() == 1 ? "animal " + refs.get(0).tag()
                    : LivestockSupport.plural(a.getHeadCount(), "animal", "animais");
            return new HealthApplicationResponse(a.getId(), a.getKind(), a.getApplicationDate(), a.getBatchId(),
                    batch != null ? batch.getName() : null, a.getHeadCount(), refs, target, a.getProductId(),
                    a.getProductName(), a.getDose(), a.getDoseUnit(), a.getTotalQuantity(), a.getTotalCost(),
                    a.getWithdrawalDays(), a.getWithdrawalUntil(), a.getNextDate(), a.getNextDescription(),
                    a.getFollowUpDoneAt() != null, a.getResponsible(), a.getNotes());
        }).toList();
    }
}
