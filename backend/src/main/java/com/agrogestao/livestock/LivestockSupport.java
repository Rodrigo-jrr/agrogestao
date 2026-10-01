package com.agrogestao.livestock;

import com.agrogestao.common.exception.BusinessException;
import com.agrogestao.common.exception.ResourceNotFoundException;
import com.agrogestao.livestock.dto.AnimalEventResponse;
import com.agrogestao.livestock.dto.AnimalResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Buscas, validações e montagem de respostas comuns a lotes, animais e compras/vendas de animais. */
@Component
public class LivestockSupport {

    private final AnimalRepository animalRepository;
    private final AnimalBatchRepository batchRepository;
    private final AnimalEventRepository eventRepository;
    private final JdbcTemplate jdbcTemplate;

    public LivestockSupport(AnimalRepository animalRepository, AnimalBatchRepository batchRepository,
            AnimalEventRepository eventRepository, JdbcTemplate jdbcTemplate) {
        this.animalRepository = animalRepository;
        this.batchRepository = batchRepository;
        this.eventRepository = eventRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * species_id do catálogo da propriedade para esta espécie (enum, ainda — ver a classe Species), criando
     * a linha na primeira vez que a propriedade usa essa espécie. Use ao salvar um Animal/AnimalBatch.
     */
    public Long ensureSpeciesId(Long propertyId, Species species) {
        Long existing = findSpeciesIdOrNull(propertyId, species);
        if (existing != null) {
            return existing;
        }
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
                "INSERT INTO species (property_id, name, allows_individual, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?)",
                propertyId, species.label(), species.allowsIndividual(), now, now);
        return findSpeciesIdOrNull(propertyId, species);
    }

    /**
     * species_id para esta espécie nesta propriedade, ou um id que não existe se a propriedade nunca usou essa
     * espécie — assim um filtro de busca por essa espécie não bate com nada em vez de ignorar o filtro (null
     * significaria "sem filtro" na query). Não cria a linha: usar só para leitura (busca/validação).
     */
    public long findSpeciesId(Long propertyId, Species species) {
        Long id = findSpeciesIdOrNull(propertyId, species);
        return id != null ? id : -1L;
    }

    private Long findSpeciesIdOrNull(Long propertyId, Species species) {
        List<Long> ids = jdbcTemplate.queryForList(
                "SELECT id FROM species WHERE property_id = ? AND LOWER(name) = LOWER(?) AND archived_at IS NULL",
                Long.class, propertyId, species.label());
        return ids.isEmpty() ? null : ids.get(0);
    }

    /** Catálogo -> enum Species, enquanto os services ainda falam o enum. */
    public Species speciesOf(Long speciesId) {
        return speciesOf(List.of(speciesId)).get(speciesId);
    }

    /** Mesma tradução, em lote (uma consulta para várias respostas). */
    public Map<Long, Species> speciesOf(Collection<Long> speciesIds) {
        List<Long> ids = speciesIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        String placeholders = ids.stream().map(id -> "?").collect(Collectors.joining(","));
        Map<Long, Species> result = new HashMap<>();
        jdbcTemplate.query("SELECT id, name FROM species WHERE id IN (" + placeholders + ")",
                rs -> {
                    result.put(rs.getLong("id"), Species.fromLabel(rs.getString("name")));
                }, ids.toArray());
        return result;
    }

    public AnimalBatch requireBatch(Long propertyId, Long batchId) {
        return batchRepository.findById(batchId)
                .filter(batch -> batch.getPropertyId().equals(propertyId))
                .orElseThrow(() -> new BusinessException("Lote inválido para esta propriedade"));
    }

    public AnimalBatch findBatch(Long propertyId, Long batchId) {
        return batchRepository.findById(batchId)
                .filter(batch -> batch.getPropertyId().equals(propertyId))
                .orElseThrow(() -> new ResourceNotFoundException("Lote não encontrado"));
    }

    /** Lote que recebe animais com brinco: individual, ativo e da mesma espécie. */
    public AnimalBatch requireIndividualBatch(Long propertyId, Long batchId, Species species) {
        AnimalBatch batch = requireBatch(propertyId, batchId);
        if (batch.isAggregate()) {
            throw new BusinessException("O lote " + batch.getName()
                    + " é controlado por contagem: ele não recebe animais com brinco.");
        }
        if (batch.isArchived()) {
            throw new BusinessException("O lote " + batch.getName() + " está arquivado.");
        }
        if (species != null && !Objects.equals(batch.getSpeciesId(), findSpeciesIdOrNull(propertyId, species))) {
            throw new BusinessException("O lote " + batch.getName() + " é de "
                    + speciesOf(batch.getSpeciesId()).label().toLowerCase()
                    + "s; o animal é " + species.label().toLowerCase() + ".");
        }
        return batch;
    }

    public AnimalBatch requireAggregateBatch(Long propertyId, Long batchId) {
        AnimalBatch batch = requireBatch(propertyId, batchId);
        if (!batch.isAggregate()) {
            throw new BusinessException("O lote " + batch.getName()
                    + " tem animais com brinco: movimente os animais, não a contagem.");
        }
        if (batch.isArchived()) {
            throw new BusinessException("O lote " + batch.getName() + " está arquivado.");
        }
        return batch;
    }

    public Animal findAnimal(Long propertyId, Long animalId) {
        return animalRepository.findById(animalId)
                .filter(animal -> animal.getPropertyId().equals(propertyId))
                .orElseThrow(() -> new ResourceNotFoundException("Animal não encontrado"));
    }

    public static void requireIndividualSpecies(Species species) {
        if (!species.allowsIndividual()) {
            throw new BusinessException(species.label() + "s são controlados só por lote (contagem de cabeças).");
        }
    }

    /** Brinco único entre os animais ativos da propriedade. */
    public void requireFreeTag(Long propertyId, String tag, Long ignoreAnimalId) {
        boolean taken = ignoreAnimalId == null
                ? animalRepository.existsByPropertyIdAndStatusAndTagIgnoreCase(propertyId, AnimalStatus.ATIVO, tag)
                : animalRepository.existsByPropertyIdAndStatusAndTagIgnoreCaseAndIdNot(
                        propertyId, AnimalStatus.ATIVO, tag, ignoreAnimalId);
        if (taken) {
            throw new BusinessException("Já existe um animal ativo com o brinco " + tag + ".");
        }
    }

    public static void requireNotFuture(LocalDate date) {
        if (date != null && date.isAfter(LocalDate.now())) {
            throw new BusinessException("A data não pode ser futura.");
        }
    }

    public AnimalEvent saveEvent(Long propertyId, AnimalEventType type, LocalDate date, Animal animal,
            AnimalBatch batch, Integer headCount, BigDecimal weight, Long tradeId, BigDecimal amount,
            String description) {
        AnimalEvent event = new AnimalEvent();
        event.setPropertyId(propertyId);
        event.setType(type);
        event.setEventDate(date);
        event.setAnimalId(animal != null ? animal.getId() : null);
        event.setBatchId(batch != null ? batch.getId() : null);
        event.setHeadCount(headCount);
        event.setWeight(weight);
        event.setTradeId(tradeId);
        event.setAmount(amount);
        event.setDescription(description);
        event.setCreatedAt(LocalDateTime.now());
        return eventRepository.save(event);
    }

    /** Cabeças por lote: contagem guardada (por contagem) ou animais ativos (individual). */
    public Map<Long, Integer> headCounts(Long propertyId, Collection<AnimalBatch> batches) {
        Map<Long, Integer> counts = new HashMap<>();
        animalRepository.countActiveByBatch(propertyId)
                .forEach(row -> counts.put((Long) row[0], ((Long) row[1]).intValue()));
        Map<Long, Integer> result = new HashMap<>();
        batches.forEach(batch -> result.put(batch.getId(),
                batch.isAggregate() ? batch.getHeadCount() : counts.getOrDefault(batch.getId(), 0)));
        return result;
    }

    public Map<Long, AnimalBatch> batchesOf(Long propertyId) {
        return batchRepository.findByPropertyIdOrderByNameAsc(propertyId).stream()
                .collect(Collectors.toMap(AnimalBatch::getId, Function.identity()));
    }

    public List<AnimalResponse> toResponses(Long propertyId, List<Animal> animals) {
        Map<Long, AnimalBatch> batches = batchesOf(propertyId);
        List<Long> motherIds = animals.stream().map(Animal::getMotherId).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> motherTags = animalRepository.findAllById(motherIds).stream()
                .collect(Collectors.toMap(Animal::getId, Animal::getTag));
        Map<Long, LocalDate> withdrawals = withdrawalsOf(animals.stream().map(Animal::getId).toList());
        Map<Long, Species> species = speciesOf(animals.stream().map(Animal::getSpeciesId).toList());
        return animals.stream().map(animal -> toResponse(animal, batches, motherTags, withdrawals, species)).toList();
    }

    public AnimalResponse toResponse(Long propertyId, Animal animal) {
        return toResponses(propertyId, List.of(animal)).get(0);
    }

    /** Fim da carência de cada animal que ainda está em carência hoje. */
    public Map<Long, LocalDate> withdrawalsOf(List<Long> animalIds) {
        Map<Long, LocalDate> result = new HashMap<>();
        if (!animalIds.isEmpty()) {
            eventRepository.withdrawalByAnimal(animalIds, LocalDate.now())
                    .forEach(row -> result.put((Long) row[0], (LocalDate) row[1]));
        }
        return result;
    }

    /** Peso atual = o da pesagem (ou entrada com peso) mais recente; sem nenhuma, fica vazio. */
    public void refreshCurrentWeight(Animal animal) {
        animal.setCurrentWeight(eventRepository.findFirstByAnimalIdAndWeightIsNotNullOrderByEventDateDescIdDesc(animal.getId())
                .map(AnimalEvent::getWeight).orElse(null));
        animalRepository.save(animal);
    }

    private static AnimalResponse toResponse(Animal a, Map<Long, AnimalBatch> batches, Map<Long, String> motherTags,
            Map<Long, LocalDate> withdrawals, Map<Long, Species> species) {
        AnimalBatch batch = a.getBatchId() != null ? batches.get(a.getBatchId()) : null;
        return new AnimalResponse(a.getId(), a.getTag(), a.getName(), species.get(a.getSpeciesId()), a.getBreed(), a.getSex(),
                a.getBirthDate(), a.isBirthDateEstimated(), a.getOrigin(), a.getEntryDate(), a.getMotherId(),
                a.getMotherId() != null ? motherTags.get(a.getMotherId()) : null, a.getSire(), a.getBatchId(),
                batch != null ? batch.getName() : null, a.getStatus(), a.getExitDate(), a.getExitReason(),
                a.getCurrentWeight(), a.getNotes(), withdrawals.get(a.getId()));
    }

    public List<AnimalEventResponse> toEventResponses(Long propertyId, List<AnimalEvent> events) {
        Map<Long, AnimalBatch> batches = batchesOf(propertyId);
        List<Long> animalIds = events.stream().map(AnimalEvent::getAnimalId).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> tags = animalRepository.findAllById(animalIds).stream()
                .collect(Collectors.toMap(Animal::getId, Animal::getTag));
        Function<Long, String> batchName = id -> id != null && batches.containsKey(id) ? batches.get(id).getName() : null;
        return events.stream().map(e -> new AnimalEventResponse(e.getId(), e.getType(), e.getEventDate(),
                e.getAnimalId(), e.getAnimalId() != null ? tags.get(e.getAnimalId()) : null, e.getBatchId(),
                batchName.apply(e.getBatchId()), e.getHeadCount(), e.getWeight(), batchName.apply(e.getFromBatchId()),
                batchName.apply(e.getToBatchId()), e.getTradeId(), e.getAmount(), e.getDescription(),
                e.getApplicationId(), e.getWithdrawalUntil())).toList();
    }

    /** Divide o total em partes iguais em centavos; a sobra do arredondamento vai para a última. */
    public static List<BigDecimal> splitAmount(BigDecimal total, int parts) {
        long cents = total.movePointRight(2).longValueExact();
        long base = cents / parts;
        return java.util.stream.IntStream.range(0, parts)
                .mapToObj(i -> BigDecimal.valueOf(i == parts - 1 ? cents - base * (parts - 1) : base, 2))
                .toList();
    }

    public static String plural(long count, String singular, String plural) {
        return count + " " + (count == 1 ? singular : plural);
    }
}
