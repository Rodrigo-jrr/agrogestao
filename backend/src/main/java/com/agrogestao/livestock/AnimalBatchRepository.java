package com.agrogestao.livestock;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnimalBatchRepository extends JpaRepository<AnimalBatch, Long> {

    List<AnimalBatch> findByPropertyIdOrderByNameAsc(Long propertyId);

    boolean existsByPropertyIdAndNameIgnoreCase(Long propertyId, String name);

    boolean existsByPropertyIdAndNameIgnoreCaseAndIdNot(Long propertyId, String name, Long id);
}
