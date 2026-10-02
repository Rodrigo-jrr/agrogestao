package com.agrogestao.agriculture;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CropHarvestRepository extends JpaRepository<CropHarvest, Long> {

    @Query("SELECT h FROM CropHarvest h WHERE h.propertyId = :propertyId "
            + "AND (:cropCycleId IS NULL OR h.cropCycleId = :cropCycleId) "
            + "AND (:fieldId IS NULL OR h.cropCycleId IN (SELECT c.id FROM CropCycle c WHERE c.fieldId = :fieldId)) "
            + "AND (:from IS NULL OR h.harvestDate >= :from) "
            + "AND (:to IS NULL OR h.harvestDate <= :to) "
            + "ORDER BY h.harvestDate DESC, h.id DESC")
    Page<CropHarvest> search(@Param("propertyId") Long propertyId, @Param("cropCycleId") Long cropCycleId,
            @Param("fieldId") Long fieldId, @Param("from") LocalDate from, @Param("to") LocalDate to,
            Pageable pageable);

    /** Todas as colheitas das safras informadas — para calcular indicadores em memória. */
    List<CropHarvest> findByPropertyIdAndCropCycleIdIn(Long propertyId, List<Long> cropCycleIds);
}
