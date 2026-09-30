package com.agrogestao.production;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductionRecordRepository extends JpaRepository<ProductionRecord, Long> {

    @Query("SELECT r FROM ProductionRecord r WHERE r.propertyId = :propertyId "
            + "AND (:from IS NULL OR r.productionDate >= :from) AND (:to IS NULL OR r.productionDate <= :to) "
            + "AND (:q IS NULL OR LOWER(r.productName) LIKE :q ESCAPE '\\') "
            + "ORDER BY r.productionDate DESC, r.id DESC")
    Page<ProductionRecord> search(
            @Param("propertyId") Long propertyId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("q") String q,
            Pageable pageable);

    List<ProductionRecord> findByPropertyIdAndProductionDateBetween(Long propertyId, LocalDate from, LocalDate to);
}
