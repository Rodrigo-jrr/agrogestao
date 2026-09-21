package com.agrogestao.production;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductionRecordRepository extends JpaRepository<ProductionRecord, Long> {

    List<ProductionRecord> findByPropertyIdOrderByProductionDateDesc(Long propertyId);

    List<ProductionRecord> findByPropertyIdAndProductionDateBetween(Long propertyId, LocalDate from, LocalDate to);
}
