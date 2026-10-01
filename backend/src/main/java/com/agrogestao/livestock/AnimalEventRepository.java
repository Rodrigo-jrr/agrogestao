package com.agrogestao.livestock;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnimalEventRepository extends JpaRepository<AnimalEvent, Long> {

    List<AnimalEvent> findByAnimalIdOrderByEventDateDescIdDesc(Long animalId);

    List<AnimalEvent> findByBatchIdOrderByEventDateDescIdDesc(Long batchId);

    List<AnimalEvent> findByTradeId(Long tradeId);

    boolean existsByAnimalIdAndTypeNot(Long animalId, AnimalEventType type);

    boolean existsByBatchIdOrFromBatchIdOrToBatchId(Long batchId, Long fromBatchId, Long toBatchId);

    List<AnimalEvent> findByAnimalIdAndTypeOrderByIdDesc(Long animalId, AnimalEventType type);
}
