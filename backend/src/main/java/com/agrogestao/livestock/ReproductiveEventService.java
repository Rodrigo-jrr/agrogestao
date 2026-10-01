package com.agrogestao.livestock;

import com.agrogestao.audit.AuditAction;
import com.agrogestao.audit.AuditService;
import com.agrogestao.common.PageResponse;
import com.agrogestao.common.exception.BusinessException;
import com.agrogestao.common.exception.ResourceNotFoundException;
import com.agrogestao.livestock.dto.AnimalRequest;
import com.agrogestao.livestock.dto.AnimalResponse;
import com.agrogestao.livestock.dto.LivestockAlerts;
import com.agrogestao.livestock.dto.OffspringInput;
import com.agrogestao.livestock.dto.ReproductiveEventRequest;
import com.agrogestao.livestock.dto.ReproductiveEventResponse;
import com.agrogestao.properties.TenantContext;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reprodução de fêmeas com brinco: cio, cobertura, inseminação, diagnóstico de prenhez, parto e aborto.
 * Mesmo desenho da sanidade — cada evento vira um {@link AnimalEventType#REPRODUCAO} na linha do tempo do
 * animal — mas a situação reprodutiva (vazia, coberta, prenhe) não é guardada à parte: é derivada daqui,
 * olhando o diagnóstico de prenhez positivo mais recente de cada fêmea e se já foi encerrado por parto/aborto.
 */
@Service
public class ReproductiveEventService {

    /** Janela mínima, em dias, para esperar o diagnóstico depois de uma cobertura ou inseminação. */
    static final int PENDING_DIAGNOSIS_DAYS = 30;

    private final ReproductiveEventRepository repository;
    private final AnimalRepository animalRepository;
    private final AnimalEventRepository eventRepository;
    private final LivestockSupport support;
    private final AnimalService animalService;
    private final TenantContext tenantContext;
    private final AuditService auditService;

    public ReproductiveEventService(
            ReproductiveEventRepository repository,
            AnimalRepository animalRepository,
            AnimalEventRepository eventRepository,
            LivestockSupport support,
            AnimalService animalService,
            TenantContext tenantContext,
            AuditService auditService) {
        this.repository = repository;
        this.animalRepository = animalRepository;
        this.eventRepository = eventRepository;
        this.support = support;
        this.animalService = animalService;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    public PageResponse<ReproductiveEventResponse> list(Long animalId, Pageable pageable) {
        Long propertyId = tenantContext.requirePropertyId();
        Page<ReproductiveEvent> page = repository.search(propertyId, animalId, pageable);
        List<ReproductiveEventResponse> content = toResponses(page.getContent());
        return new PageResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Transactional
    public ReproductiveEventResponse register(ReproductiveEventRequest request) {
        tenantContext.requireWriteAccess();
        Long propertyId = tenantContext.requirePropertyId();
        LivestockSupport.requireNotFuture(request.date());
        Animal animal = support.findAnimal(propertyId, request.animalId());
        if (animal.getSex() != Sex.FEMEA) {
            throw new BusinessException("Reprodução só se aplica a fêmeas.");
        }
        if (!animal.isActive()) {
            throw new BusinessException("O animal " + animal.getTag() + " não está no rebanho.");
        }
        if (request.date().isBefore(animal.getEntryDate())) {
            throw new BusinessException("A data é anterior à entrada do animal " + animal.getTag() + ".");
        }
        SpeciesCatalog species = support.speciesOf(animal.getSpeciesId());
        LivestockSupport.requireIndividualSpecies(species);

        ReproductiveEvent event = new ReproductiveEvent();
        event.setPropertyId(propertyId);
        event.setAnimalId(animal.getId());
        event.setKind(request.kind());
        event.setEventDate(request.date());
        event.setNotes(AnimalBatchService.blankToNull(request.notes()));

        List<Long> offspringIds = List.of();
        switch (request.kind()) {
            case CIO -> {
                // Sem campos específicos: só marca o cio na linha do tempo.
            }
            case COBERTURA, INSEMINACAO -> event.setSire(AnimalBatchService.blankToNull(request.sire()));
            case DIAGNOSTICO_PRENHEZ -> applyDiagnosis(propertyId, animal, request, event);
            case PARTO -> offspringIds = applyBirth(propertyId, animal, request, event);
            case ABORTO -> applyLossOfPregnancy(propertyId, animal, event);
        }

        repository.save(event);
        AnimalEvent timelineEvent = support.saveEvent(propertyId, AnimalEventType.REPRODUCAO, request.date(), animal,
                null, null, null, null, null, describe(event));
        timelineEvent.setReproductiveEventId(event.getId());

        auditService.record("ReproductiveEvent", event.getId(), AuditAction.CREATE,
                animal.getTag() + " — " + describe(event));
        return toResponse(event, animal.getTag(), offspringIds);
    }

    /** Desfaz um lançamento feito por engano. Partos e abortos não podem ser desfeitos (já criaram crias ou encerraram a gestação). */
    @Transactional
    public void delete(Long id) {
        tenantContext.requireManageAccess();
        Long propertyId = tenantContext.requirePropertyId();
        ReproductiveEvent event = findOwned(propertyId, id);
        if (event.getKind() == ReproductiveEventKind.PARTO || event.getKind() == ReproductiveEventKind.ABORTO) {
            throw new BusinessException("Partos e abortos não podem ser excluídos. Corrija o cadastro das crias, "
                    + "se for o caso, ou registre a saída do animal.");
        }
        if (repository.existsByDiagnosisEventId(id)) {
            throw new BusinessException("Esta gestação já tem parto ou aborto registrado: desfaça-o primeiro.");
        }
        Animal animal = animalRepository.findById(event.getAnimalId()).orElse(null);
        eventRepository.deleteAll(eventRepository.findByReproductiveEventId(id));
        repository.delete(event);
        auditService.record("ReproductiveEvent", id, AuditAction.DELETE,
                (animal != null ? animal.getTag() : "") + " — " + describe(event) + " (excluído)");
    }

    private void applyDiagnosis(Long propertyId, Animal animal, ReproductiveEventRequest request, ReproductiveEvent event) {
        if (request.pregnancyResult() == null) {
            throw new BusinessException("Informe o resultado do diagnóstico (positivo ou negativo).");
        }
        if (repository.findOpenPregnancy(propertyId, animal.getId()).isPresent()) {
            throw new BusinessException("A fêmea " + animal.getTag() + " já tem uma gestação em aberto.");
        }
        event.setPregnancyResult(request.pregnancyResult());
        if (request.pregnancyResult() == PregnancyResult.POSITIVO) {
            if (request.expectedCalvingDate() == null) {
                throw new BusinessException("Informe a previsão do parto.");
            }
            if (!request.expectedCalvingDate().isAfter(request.date())) {
                throw new BusinessException("A previsão do parto deve ser depois da data do diagnóstico.");
            }
            event.setExpectedCalvingDate(request.expectedCalvingDate());
        }
    }

    private List<Long> applyBirth(Long propertyId, Animal mother, ReproductiveEventRequest request, ReproductiveEvent event) {
        ReproductiveEvent diagnosis = requireOpenPregnancy(propertyId, mother);
        if (request.date().isBefore(diagnosis.getEventDate())) {
            throw new BusinessException("O parto não pode ser antes do diagnóstico de prenhez.");
        }
        event.setDiagnosisEventId(diagnosis.getId());
        List<OffspringInput> offspring = request.offspring() != null ? request.offspring() : List.of();
        if (offspring.isEmpty()) {
            throw new BusinessException("Informe ao menos uma cria nascida.");
        }
        event.setOffspringCount(offspring.size());

        Long batchId = request.batchId() != null ? request.batchId() : mother.getBatchId();
        String sire = sireOfPregnancy(diagnosis);
        List<Long> ids = new ArrayList<>();
        for (OffspringInput input : offspring) {
            AnimalRequest animalRequest = new AnimalRequest(input.tag(), input.name(), mother.getSpeciesId(),
                    input.breed(), input.sex(), request.date(), false, AnimalOrigin.NASCIMENTO, request.date(),
                    mother.getId(), sire, batchId, input.weight(), null);
            AnimalResponse created = animalService.create(animalRequest);
            ids.add(created.id());
        }
        return ids;
    }

    /** Touro/sêmen da cobertura ou inseminação que originou esta gestação, se tiver sido registrado. */
    private String sireOfPregnancy(ReproductiveEvent diagnosis) {
        return repository.findByAnimalIdOrderByEventDateDescIdDesc(diagnosis.getAnimalId()).stream()
                .filter(e -> (e.getKind() == ReproductiveEventKind.COBERTURA || e.getKind() == ReproductiveEventKind.INSEMINACAO)
                        && !e.getEventDate().isAfter(diagnosis.getEventDate()) && e.getSire() != null)
                .findFirst()
                .map(ReproductiveEvent::getSire)
                .orElse(null);
    }

    private void applyLossOfPregnancy(Long propertyId, Animal animal, ReproductiveEvent event) {
        ReproductiveEvent diagnosis = requireOpenPregnancy(propertyId, animal);
        if (event.getEventDate().isBefore(diagnosis.getEventDate())) {
            throw new BusinessException("O aborto não pode ser antes do diagnóstico de prenhez.");
        }
        event.setDiagnosisEventId(diagnosis.getId());
    }

    private ReproductiveEvent requireOpenPregnancy(Long propertyId, Animal animal) {
        return repository.findOpenPregnancy(propertyId, animal.getId())
                .orElseThrow(() -> new BusinessException("A fêmea " + animal.getTag()
                        + " não tem diagnóstico de prenhez positivo em aberto."));
    }

    private ReproductiveEvent findOwned(Long propertyId, Long id) {
        return repository.findById(id)
                .filter(e -> e.getPropertyId().equals(propertyId))
                .orElseThrow(() -> new ResourceNotFoundException("Evento reprodutivo não encontrado"));
    }

    /** Partos previstos nos próximos dias (e os já atrasados) — mesma janela dos alertas de sanidade. */
    public List<LivestockAlerts.UpcomingCalving> upcomingCalvings() {
        Long propertyId = tenantContext.requirePropertyId();
        LocalDate until = LocalDate.now().plusDays(HealthService.ALERT_WINDOW_DAYS);
        List<ReproductiveEvent> open = repository.findUpcomingCalvings(propertyId, until);
        Map<Long, String> tags = tagsOf(open.stream().map(ReproductiveEvent::getAnimalId).toList());
        return open.stream()
                .map(e -> new LivestockAlerts.UpcomingCalving(e.getAnimalId(), tags.get(e.getAnimalId()), e.getExpectedCalvingDate()))
                .sorted(Comparator.comparing(LivestockAlerts.UpcomingCalving::expectedCalvingDate))
                .toList();
    }

    /** Fêmeas cobertas ou inseminadas há {@link #PENDING_DIAGNOSIS_DAYS} dias ou mais, ainda sem diagnóstico. */
    public List<LivestockAlerts.PendingDiagnosis> pendingDiagnoses() {
        Long propertyId = tenantContext.requirePropertyId();
        LocalDate threshold = LocalDate.now().minusDays(PENDING_DIAGNOSIS_DAYS);
        Map<Long, ReproductiveEvent> latestByAnimal = new LinkedHashMap<>();
        for (ReproductiveEvent e : repository.findByPropertyIdOrderByAnimalIdAscEventDateDescIdDesc(propertyId)) {
            latestByAnimal.putIfAbsent(e.getAnimalId(), e);
        }
        List<ReproductiveEvent> pending = latestByAnimal.values().stream()
                .filter(e -> (e.getKind() == ReproductiveEventKind.COBERTURA || e.getKind() == ReproductiveEventKind.INSEMINACAO)
                        && !e.getEventDate().isAfter(threshold))
                .toList();
        Map<Long, String> tags = tagsOf(pending.stream().map(ReproductiveEvent::getAnimalId).toList());
        return pending.stream()
                .map(e -> new LivestockAlerts.PendingDiagnosis(e.getAnimalId(), tags.get(e.getAnimalId()), e.getEventDate()))
                .sorted(Comparator.comparing(LivestockAlerts.PendingDiagnosis::since))
                .toList();
    }

    private Map<Long, String> tagsOf(List<Long> animalIds) {
        Map<Long, String> tags = new LinkedHashMap<>();
        animalRepository.findAllById(animalIds).forEach(a -> tags.put(a.getId(), a.getTag()));
        return tags;
    }

    private List<ReproductiveEventResponse> toResponses(List<ReproductiveEvent> events) {
        Map<Long, String> tags = tagsOf(events.stream().map(ReproductiveEvent::getAnimalId).distinct().toList());
        return events.stream().map(e -> toResponse(e, tags.get(e.getAnimalId()),
                e.getKind() == ReproductiveEventKind.PARTO ? offspringOf(e.getId()) : List.of())).toList();
    }

    /** Crias nascidas neste parto: animais cuja mãe é a fêmea do evento e cujo nascimento é a data do parto. */
    private List<Long> offspringOf(Long reproductiveEventId) {
        return eventRepository.findByReproductiveEventId(reproductiveEventId).stream()
                .findFirst()
                .map(timeline -> animalRepository.findByMotherIdOrderByBirthDateAsc(timeline.getAnimalId()).stream()
                        .filter(a -> a.getBirthDate() != null && a.getBirthDate().isEqual(timeline.getEventDate()))
                        .map(Animal::getId)
                        .toList())
                .orElse(List.of());
    }

    private static ReproductiveEventResponse toResponse(ReproductiveEvent e, String animalTag, List<Long> offspringIds) {
        return new ReproductiveEventResponse(e.getId(), e.getAnimalId(), animalTag, e.getKind(), e.getEventDate(),
                e.getSire(), e.getPregnancyResult(), e.getExpectedCalvingDate(), e.getDiagnosisEventId(),
                e.getOffspringCount(), offspringIds, e.getNotes(), e.getClosedWithoutBirthAt());
    }

    private static String describe(ReproductiveEvent e) {
        return switch (e.getKind()) {
            case CIO -> "Cio";
            case COBERTURA -> "Cobertura" + (e.getSire() != null ? " — " + e.getSire() : "");
            case INSEMINACAO -> "Inseminação" + (e.getSire() != null ? " — " + e.getSire() : "");
            case DIAGNOSTICO_PRENHEZ -> "Diagnóstico de prenhez: " + (e.getPregnancyResult() == PregnancyResult.POSITIVO
                    ? "positiva (parto previsto " + e.getExpectedCalvingDate() + ")" : "negativa");
            case PARTO -> "Parto — " + LivestockSupport.plural(
                    e.getOffspringCount() != null ? e.getOffspringCount() : 0, "cria", "crias");
            case ABORTO -> "Aborto";
        };
    }
}
