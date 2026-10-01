package com.agrogestao.livestock;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReproductiveEventRepository extends JpaRepository<ReproductiveEvent, Long> {

    List<ReproductiveEvent> findByAnimalIdOrderByEventDateDescIdDesc(Long animalId);

    @Query("SELECT e FROM ReproductiveEvent e WHERE e.propertyId = :propertyId "
            + "AND (:animalId IS NULL OR e.animalId = :animalId) ORDER BY e.eventDate DESC, e.id DESC")
    Page<ReproductiveEvent> search(@Param("propertyId") Long propertyId, @Param("animalId") Long animalId, Pageable pageable);

    /** Todos os eventos da propriedade, do mais recente para o mais antigo dentro de cada animal. */
    List<ReproductiveEvent> findByPropertyIdOrderByAnimalIdAscEventDateDescIdDesc(Long propertyId);

    boolean existsByAnimalId(Long animalId);

    boolean existsByDiagnosisEventId(Long diagnosisEventId);

    /** Gestação (diagnóstico positivo) em aberto desta fêmea, se houver — no máximo uma por vez. */
    @Query("SELECT d FROM ReproductiveEvent d WHERE d.propertyId = :propertyId AND d.animalId = :animalId "
            + "AND d.kind = com.agrogestao.livestock.ReproductiveEventKind.DIAGNOSTICO_PRENHEZ "
            + "AND d.pregnancyResult = com.agrogestao.livestock.PregnancyResult.POSITIVO "
            + "AND NOT EXISTS (SELECT 1 FROM ReproductiveEvent closing WHERE closing.diagnosisEventId = d.id)")
    Optional<ReproductiveEvent> findOpenPregnancy(@Param("propertyId") Long propertyId, @Param("animalId") Long animalId);

    /**
     * Diagnósticos de prenhez positivos ainda em aberto (sem parto/aborto que os encerre) — a gestação
     * atual de cada fêmea prenhe. Base para a situação reprodutiva e para o alerta de partos previstos.
     */
    @Query("SELECT d FROM ReproductiveEvent d WHERE d.propertyId = :propertyId "
            + "AND d.kind = com.agrogestao.livestock.ReproductiveEventKind.DIAGNOSTICO_PRENHEZ "
            + "AND d.pregnancyResult = com.agrogestao.livestock.PregnancyResult.POSITIVO "
            + "AND NOT EXISTS (SELECT 1 FROM ReproductiveEvent closing WHERE closing.diagnosisEventId = d.id)")
    List<ReproductiveEvent> findOpenPregnancies(@Param("propertyId") Long propertyId);

    /** Gestações em aberto com previsão de parto até {@code until} (inclui as já atrasadas) — alerta de parto. */
    @Query("SELECT d FROM ReproductiveEvent d WHERE d.propertyId = :propertyId "
            + "AND d.kind = com.agrogestao.livestock.ReproductiveEventKind.DIAGNOSTICO_PRENHEZ "
            + "AND d.pregnancyResult = com.agrogestao.livestock.PregnancyResult.POSITIVO "
            + "AND d.expectedCalvingDate IS NOT NULL AND d.expectedCalvingDate <= :until "
            + "AND NOT EXISTS (SELECT 1 FROM ReproductiveEvent closing WHERE closing.diagnosisEventId = d.id)")
    List<ReproductiveEvent> findUpcomingCalvings(@Param("propertyId") Long propertyId, @Param("until") LocalDate until);
}
