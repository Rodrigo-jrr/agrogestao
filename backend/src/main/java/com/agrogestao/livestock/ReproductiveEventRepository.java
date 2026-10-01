package com.agrogestao.livestock;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReproductiveEventRepository extends JpaRepository<ReproductiveEvent, Long> {

    List<ReproductiveEvent> findByAnimalIdOrderByEventDateDescIdDesc(Long animalId);

    boolean existsByAnimalId(Long animalId);

    boolean existsByDiagnosisEventId(Long diagnosisEventId);

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
