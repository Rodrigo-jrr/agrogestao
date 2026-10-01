package com.agrogestao.livestock;

import com.agrogestao.audit.AuditAction;
import com.agrogestao.audit.AuditService;
import com.agrogestao.common.PageResponse;
import com.agrogestao.common.exception.BusinessException;
import com.agrogestao.livestock.dto.AnimalDetailResponse;
import com.agrogestao.livestock.dto.AnimalExitRequest;
import com.agrogestao.livestock.dto.AnimalRequest;
import com.agrogestao.livestock.dto.AnimalResponse;
import com.agrogestao.livestock.dto.MoveAnimalsRequest;
import com.agrogestao.properties.TenantContext;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Animais com brinco. Entrada por nascimento ou outra origem aqui; por compra, em {@link LivestockTradeService}
 * (que gera o financeiro). Lote, situação e peso mudam por eventos, que formam a linha do tempo do animal.
 */
@Service
public class AnimalService {

    private final AnimalRepository animalRepository;
    private final AnimalEventRepository eventRepository;
    private final LivestockSupport support;
    private final TenantContext tenantContext;
    private final AuditService auditService;

    public AnimalService(
            AnimalRepository animalRepository,
            AnimalEventRepository eventRepository,
            LivestockSupport support,
            TenantContext tenantContext,
            AuditService auditService) {
        this.animalRepository = animalRepository;
        this.eventRepository = eventRepository;
        this.support = support;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    public PageResponse<AnimalResponse> search(AnimalStatus status, Long batchId, Long speciesId, String query,
            Pageable pageable) {
        Long propertyId = tenantContext.requirePropertyId();
        Page<Animal> page = animalRepository.search(propertyId, status, batchId, speciesId,
                PageResponse.likePattern(query), pageable);
        List<AnimalResponse> content = support.toResponses(propertyId, page.getContent());
        return new PageResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    public AnimalDetailResponse detail(Long id) {
        Long propertyId = tenantContext.requirePropertyId();
        Animal animal = support.findAnimal(propertyId, id);
        return new AnimalDetailResponse(support.toResponse(propertyId, animal),
                support.toEventResponses(propertyId, eventRepository.findByAnimalIdOrderByEventDateDescIdDesc(id)),
                support.toResponses(propertyId, animalRepository.findByMotherIdOrderByBirthDateAsc(id)));
    }

    @Transactional
    public AnimalResponse create(AnimalRequest request) {
        tenantContext.requireWriteAccess();
        Long propertyId = tenantContext.requirePropertyId();
        SpeciesCatalog species = support.requireActiveSpecies(propertyId, request.speciesId());
        LivestockSupport.requireIndividualSpecies(species);
        AnimalOrigin origin = request.origin() != null ? request.origin() : AnimalOrigin.OUTRA;
        if (origin == AnimalOrigin.COMPRA) {
            throw new BusinessException("Animais comprados entram pela compra de animais, que também lança o pagamento.");
        }
        LocalDate entryDate = request.entryDate() != null ? request.entryDate()
                : origin == AnimalOrigin.NASCIMENTO && request.birthDate() != null ? request.birthDate() : LocalDate.now();
        LivestockSupport.requireNotFuture(entryDate);
        validateBirth(request.birthDate(), entryDate);
        String tag = request.tag().trim();
        support.requireFreeTag(propertyId, tag, null);
        AnimalBatch batch = request.batchId() != null
                ? support.requireIndividualBatch(propertyId, request.batchId(), species.getId()) : null;

        Animal animal = new Animal();
        animal.setPropertyId(propertyId);
        animal.setTag(tag);
        animal.setSpeciesId(species.getId());
        animal.setOrigin(origin);
        animal.setEntryDate(entryDate);
        animal.setBatchId(batch != null ? batch.getId() : null);
        animal.setCurrentWeight(request.weight());
        applyEditableFields(propertyId, animal, request);
        animalRepository.save(animal);

        support.saveEvent(propertyId, AnimalEventType.ENTRADA, entryDate, animal, batch, null, request.weight(),
                null, null, origin == AnimalOrigin.NASCIMENTO ? "Nascimento" : "Entrada");
        auditService.record("Animal", animal.getId(), AuditAction.CREATE, describe(animal, species));
        return support.toResponse(propertyId, animal);
    }

    /** Dados cadastrais. Lote, situação e peso mudam por eventos; a espécie não muda. */
    @Transactional
    public AnimalResponse update(Long id, AnimalRequest request) {
        tenantContext.requireWriteAccess();
        Long propertyId = tenantContext.requirePropertyId();
        Animal animal = support.findAnimal(propertyId, id);
        if (!animal.getSpeciesId().equals(request.speciesId())) {
            throw new BusinessException("A espécie não pode ser alterada. Se foi um engano, exclua e cadastre de novo.");
        }
        validateBirth(request.birthDate(), animal.getEntryDate());
        String tag = request.tag().trim();
        if (animal.isActive()) {
            support.requireFreeTag(propertyId, tag, id);
        }
        if (request.sex() == Sex.MACHO && animal.getSex() == Sex.FEMEA && animalRepository.existsByMotherId(id)) {
            throw new BusinessException("Este animal está registrado como mãe de outros animais.");
        }
        animal.setTag(tag);
        applyEditableFields(propertyId, animal, request);
        animalRepository.save(animal);
        auditService.record("Animal", animal.getId(), AuditAction.UPDATE, describe(animal, support.speciesOf(animal.getSpeciesId())));
        return support.toResponse(propertyId, animal);
    }

    /** Exclui um cadastro feito por engano: só sem movimentações, sem crias e fora de compra/venda. */
    @Transactional
    public void delete(Long id) {
        tenantContext.requireManageAccess();
        Long propertyId = tenantContext.requirePropertyId();
        Animal animal = support.findAnimal(propertyId, id);
        List<AnimalEvent> events = eventRepository.findByAnimalIdOrderByEventDateDescIdDesc(id);
        if (events.stream().anyMatch(e -> e.getType() != AnimalEventType.ENTRADA || e.getTradeId() != null)
                || animalRepository.existsByMotherId(id)) {
            throw new BusinessException("O animal " + animal.getTag() + " tem movimentações, crias ou veio de uma compra: "
                    + "não pode ser excluído. Registre a saída em vez disso.");
        }
        eventRepository.deleteAll(events);
        eventRepository.flush();
        SpeciesCatalog species = support.speciesOf(animal.getSpeciesId());
        animalRepository.delete(animal);
        auditService.record("Animal", animal.getId(), AuditAction.DELETE, describe(animal, species));
    }

    @Transactional
    public List<AnimalResponse> move(MoveAnimalsRequest request) {
        tenantContext.requireWriteAccess();
        Long propertyId = tenantContext.requirePropertyId();
        LivestockSupport.requireNotFuture(request.date());
        List<Long> ids = List.copyOf(new LinkedHashSet<>(request.animalIds()));
        List<Animal> animals = animalRepository.findByPropertyIdAndIdIn(propertyId, ids);
        if (animals.size() != ids.size()) {
            throw new BusinessException("Algum animal selecionado não pertence a esta propriedade.");
        }
        AnimalBatch target = request.toBatchId() != null
                ? support.requireIndividualBatch(propertyId, request.toBatchId(), null) : null;
        for (Animal animal : animals) {
            if (!animal.isActive()) {
                throw new BusinessException("O animal " + animal.getTag() + " não está ativo.");
            }
            if (target != null && !Objects.equals(target.getSpeciesId(), animal.getSpeciesId())) {
                throw new BusinessException("O lote " + target.getName() + " é de "
                        + support.speciesOf(target.getSpeciesId()).getName().toLowerCase() + "s; o animal " + animal.getTag()
                        + " é " + support.speciesOf(animal.getSpeciesId()).getName().toLowerCase() + ".");
            }
            if (request.date().isBefore(animal.getEntryDate())) {
                throw new BusinessException("A data é anterior à entrada do animal " + animal.getTag() + ".");
            }
        }
        for (Animal animal : animals) {
            Long from = animal.getBatchId();
            Long to = target != null ? target.getId() : null;
            if (Objects.equals(from, to)) {
                continue;
            }
            animal.setBatchId(to);
            animalRepository.save(animal);
            AnimalEvent event = support.saveEvent(propertyId, AnimalEventType.MUDANCA_LOTE, request.date(), animal,
                    null, null, null, null, null, null);
            event.setFromBatchId(from);
            event.setToBatchId(to);
        }
        auditService.record("Animal", animals.get(0).getId(), AuditAction.UPDATE, "%s movido(s) para %s".formatted(
                LivestockSupport.plural(animals.size(), "animal", "animais"), target != null ? target.getName() : "sem lote"));
        return support.toResponses(propertyId, animals);
    }

    /** Saída sem venda (morte, descarte...). */
    @Transactional
    public AnimalResponse exit(Long id, AnimalExitRequest request) {
        tenantContext.requireWriteAccess();
        Long propertyId = tenantContext.requirePropertyId();
        Animal animal = support.findAnimal(propertyId, id);
        if (!animal.isActive()) {
            throw new BusinessException("O animal " + animal.getTag() + " já saiu do rebanho.");
        }
        LivestockSupport.requireNotFuture(request.date());
        if (request.date().isBefore(animal.getEntryDate())) {
            throw new BusinessException("A data da saída é anterior à entrada do animal.");
        }
        String reason = request.type().label()
                + (AnimalBatchService.blankToNull(request.reason()) != null ? ": " + request.reason().trim() : "");
        animal.setStatus(request.type().status());
        animal.setExitDate(request.date());
        animal.setExitReason(reason);
        animalRepository.save(animal);
        support.saveEvent(propertyId, AnimalEventType.SAIDA, request.date(), animal, null, null, null, null, null, reason);
        auditService.record("Animal", animal.getId(), AuditAction.UPDATE, animal.getTag() + " — " + reason);
        return support.toResponse(propertyId, animal);
    }

    /** Desfaz uma saída registrada por engano (morte/descarte). Vendas se desfazem cancelando a venda. */
    @Transactional
    public AnimalResponse reactivate(Long id) {
        tenantContext.requireManageAccess();
        Long propertyId = tenantContext.requirePropertyId();
        Animal animal = support.findAnimal(propertyId, id);
        if (animal.isActive()) {
            return support.toResponse(propertyId, animal);
        }
        if (animal.getStatus() == AnimalStatus.VENDIDO) {
            throw new BusinessException("Para desfazer uma venda, cancele a venda de animais.");
        }
        support.requireFreeTag(propertyId, animal.getTag(), id);
        eventRepository.findByAnimalIdAndTypeOrderByIdDesc(id, AnimalEventType.SAIDA).stream()
                .filter(e -> e.getTradeId() == null)
                .findFirst()
                .ifPresent(eventRepository::delete);
        animal.setStatus(AnimalStatus.ATIVO);
        animal.setExitDate(null);
        animal.setExitReason(null);
        animalRepository.save(animal);
        auditService.record("Animal", animal.getId(), AuditAction.UPDATE, animal.getTag() + " — saída desfeita");
        return support.toResponse(propertyId, animal);
    }

    private void applyEditableFields(Long propertyId, Animal animal, AnimalRequest request) {
        animal.setName(AnimalBatchService.blankToNull(request.name()));
        animal.setBreed(AnimalBatchService.blankToNull(request.breed()));
        animal.setSex(request.sex());
        animal.setBirthDate(request.birthDate());
        animal.setBirthDateEstimated(request.birthDate() != null && Boolean.TRUE.equals(request.birthDateEstimated()));
        animal.setSire(AnimalBatchService.blankToNull(request.sire()));
        animal.setNotes(AnimalBatchService.blankToNull(request.notes()));
        animal.setMotherId(validMother(propertyId, animal, request.motherId()));
    }

    private Long validMother(Long propertyId, Animal child, Long motherId) {
        if (motherId == null) {
            return null;
        }
        if (motherId.equals(child.getId())) {
            throw new BusinessException("Um animal não pode ser mãe de si mesmo.");
        }
        Animal mother = animalRepository.findById(motherId)
                .filter(m -> m.getPropertyId().equals(propertyId))
                .orElseThrow(() -> new BusinessException("Mãe inválida para esta propriedade"));
        if (mother.getSex() != Sex.FEMEA || !Objects.equals(mother.getSpeciesId(), child.getSpeciesId())) {
            throw new BusinessException("A mãe precisa ser uma fêmea da mesma espécie.");
        }
        if (mother.getBirthDate() != null && child.getBirthDate() != null
                && !mother.getBirthDate().isBefore(child.getBirthDate())) {
            throw new BusinessException("A mãe precisa ter nascido antes da cria.");
        }
        return motherId;
    }

    private static void validateBirth(LocalDate birthDate, LocalDate entryDate) {
        LivestockSupport.requireNotFuture(birthDate);
        if (birthDate != null && entryDate != null && birthDate.isAfter(entryDate)) {
            throw new BusinessException("O nascimento não pode ser depois da entrada na propriedade.");
        }
    }

    private static String describe(Animal animal, SpeciesCatalog species) {
        return "%s (%s, %s)".formatted(animal.getTag(), species.getName().toLowerCase(),
                animal.getSex() == Sex.FEMEA ? "fêmea" : "macho");
    }
}
