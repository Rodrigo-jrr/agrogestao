package com.agrogestao.livestock;

import com.agrogestao.audit.AuditAction;
import com.agrogestao.audit.AuditService;
import com.agrogestao.common.exception.BusinessException;
import com.agrogestao.common.exception.ResourceNotFoundException;
import com.agrogestao.livestock.dto.AnimalEventResponse;
import com.agrogestao.livestock.dto.WeighingRequest;
import com.agrogestao.properties.TenantContext;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pesagens: de cada animal (uma ou várias de uma vez, ex.: o lote no tronco) ou o peso médio de uma amostra
 * de um lote por contagem. O peso atual do animal é sempre o da pesagem mais recente.
 */
@Service
public class WeighingService {

    private final AnimalRepository animalRepository;
    private final AnimalEventRepository eventRepository;
    private final LivestockSupport support;
    private final TenantContext tenantContext;
    private final AuditService auditService;

    public WeighingService(AnimalRepository animalRepository, AnimalEventRepository eventRepository,
            LivestockSupport support, TenantContext tenantContext, AuditService auditService) {
        this.animalRepository = animalRepository;
        this.eventRepository = eventRepository;
        this.support = support;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    @Transactional
    public List<AnimalEventResponse> register(WeighingRequest request) {
        tenantContext.requireWriteAccess();
        Long propertyId = tenantContext.requirePropertyId();
        LivestockSupport.requireNotFuture(request.date());
        boolean individual = request.entries() != null && !request.entries().isEmpty();
        if (individual == (request.batchId() != null)) {
            throw new BusinessException("Informe o peso dos animais ou o peso médio de um lote por contagem.");
        }
        return individual ? weighAnimals(propertyId, request) : weighBatch(propertyId, request);
    }

    private List<AnimalEventResponse> weighAnimals(Long propertyId, WeighingRequest request) {
        Set<Long> seen = new HashSet<>();
        for (WeighingRequest.Entry entry : request.entries()) {
            if (!seen.add(entry.animalId())) {
                throw new BusinessException("Um animal aparece mais de uma vez na pesagem.");
            }
        }
        Map<Long, Animal> animals = animalRepository.findByPropertyIdAndIdIn(propertyId, List.copyOf(seen)).stream()
                .collect(Collectors.toMap(Animal::getId, Function.identity()));
        if (animals.size() != seen.size()) {
            throw new BusinessException("Algum animal da pesagem não pertence a esta propriedade.");
        }
        List<AnimalEvent> events = request.entries().stream().map(entry -> {
            Animal animal = animals.get(entry.animalId());
            if (!animal.isActive()) {
                throw new BusinessException("O animal " + animal.getTag() + " não está no rebanho.");
            }
            if (request.date().isBefore(animal.getEntryDate())) {
                throw new BusinessException("A pesagem é anterior à entrada do animal " + animal.getTag() + ".");
            }
            AnimalEvent event = support.saveEvent(propertyId, AnimalEventType.PESAGEM, request.date(), animal, null,
                    null, entry.weight(), null, null, null);
            support.refreshCurrentWeight(animal);
            return event;
        }).toList();
        auditService.record("Animal", events.get(0).getAnimalId(), AuditAction.UPDATE, events.size() == 1
                ? "pesagem de %s: %s kg".formatted(animals.get(events.get(0).getAnimalId()).getTag(),
                        events.get(0).getWeight().stripTrailingZeros().toPlainString())
                : "pesagem de " + LivestockSupport.plural(events.size(), "animal", "animais"));
        return support.toEventResponses(propertyId, events);
    }

    private List<AnimalEventResponse> weighBatch(Long propertyId, WeighingRequest request) {
        AnimalBatch batch = support.requireAggregateBatch(propertyId, request.batchId());
        if (request.averageWeight() == null || request.sampleSize() == null) {
            throw new BusinessException("Informe o peso médio e quantos animais foram pesados na amostra.");
        }
        AnimalEvent event = support.saveEvent(propertyId, AnimalEventType.PESAGEM, request.date(), null, batch,
                request.sampleSize(), request.averageWeight().setScale(2, RoundingMode.HALF_UP), null, null,
                "Peso médio de uma amostra de " + LivestockSupport.plural(request.sampleSize(), "animal", "animais"));
        auditService.record("AnimalBatch", batch.getId(), AuditAction.UPDATE, "%s: peso médio %s kg".formatted(
                batch.getName(), event.getWeight().stripTrailingZeros().toPlainString()));
        return support.toEventResponses(propertyId, List.of(event));
    }

    /** Exclui uma pesagem lançada errada; o peso atual volta a ser o da anterior. */
    @Transactional
    public void delete(Long eventId) {
        tenantContext.requireManageAccess();
        Long propertyId = tenantContext.requirePropertyId();
        AnimalEvent event = eventRepository.findById(eventId)
                .filter(e -> e.getPropertyId().equals(propertyId) && e.getType() == AnimalEventType.PESAGEM)
                .orElseThrow(() -> new ResourceNotFoundException("Pesagem não encontrada"));
        eventRepository.delete(event);
        eventRepository.flush();
        if (event.getAnimalId() != null) {
            Animal animal = support.findAnimal(propertyId, event.getAnimalId());
            support.refreshCurrentWeight(animal);
            auditService.record("Animal", animal.getId(), AuditAction.UPDATE, "pesagem excluída de " + animal.getTag());
        } else {
            auditService.record("AnimalBatch", event.getBatchId(), AuditAction.UPDATE, "pesagem do lote excluída");
        }
    }
}
