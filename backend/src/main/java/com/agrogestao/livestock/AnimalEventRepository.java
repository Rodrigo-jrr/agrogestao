package com.agrogestao.livestock;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnimalEventRepository extends JpaRepository<AnimalEvent, Long> {

    List<AnimalEvent> findByAnimalIdOrderByEventDateDescIdDesc(Long animalId);

    List<AnimalEvent> findByBatchIdOrderByEventDateDescIdDesc(Long batchId);

    List<AnimalEvent> findByTradeId(Long tradeId);

    List<AnimalEvent> findByApplicationId(Long applicationId);

    List<AnimalEvent> findByReproductiveEventId(Long reproductiveEventId);

    /** Eventos de um tipo no período — base para os indicadores (custo, mortalidade, GMD). */
    List<AnimalEvent> findByPropertyIdAndTypeAndEventDateBetween(
            Long propertyId, AnimalEventType type, LocalDate from, LocalDate to);

    boolean existsByAnimalIdAndTypeNot(Long animalId, AnimalEventType type);

    boolean existsByBatchIdOrFromBatchIdOrToBatchId(Long batchId, Long fromBatchId, Long toBatchId);

    List<AnimalEvent> findByAnimalIdAndTypeOrderByIdDesc(Long animalId, AnimalEventType type);

    /** Último peso registrado do animal (entrada ou pesagem), para o peso atual. */
    Optional<AnimalEvent> findFirstByAnimalIdAndWeightIsNotNullOrderByEventDateDescIdDesc(Long animalId);

    /** [animalId, fim da carência] dos animais informados que ainda estão em carência. */
    @Query("SELECT e.animalId, MAX(e.withdrawalUntil) FROM AnimalEvent e "
            + "WHERE e.animalId IN :ids AND e.withdrawalUntil >= :today GROUP BY e.animalId")
    List<Object[]> withdrawalByAnimal(@Param("ids") Collection<Long> ids, @Param("today") LocalDate today);

    /** [animalId, fim da carência] de todos os animais ativos em carência na propriedade. */
    @Query("SELECT e.animalId, MAX(e.withdrawalUntil) FROM AnimalEvent e, Animal a "
            + "WHERE a.id = e.animalId AND e.propertyId = :propertyId AND e.withdrawalUntil >= :today "
            + "AND a.status = com.agrogestao.livestock.AnimalStatus.ATIVO GROUP BY e.animalId")
    List<Object[]> activeWithdrawals(@Param("propertyId") Long propertyId, @Param("today") LocalDate today);

    /** [batchId, fim da carência] dos lotes por contagem em carência. */
    @Query("SELECT e.batchId, MAX(e.withdrawalUntil) FROM AnimalEvent e WHERE e.propertyId = :propertyId "
            + "AND e.animalId IS NULL AND e.batchId IS NOT NULL AND e.withdrawalUntil >= :today GROUP BY e.batchId")
    List<Object[]> batchWithdrawals(@Param("propertyId") Long propertyId, @Param("today") LocalDate today);
}
