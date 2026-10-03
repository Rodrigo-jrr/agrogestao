package com.agrogestao.agriculture;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CropOperationRepository extends JpaRepository<CropOperation, Long> {

    @Query("SELECT o FROM CropOperation o WHERE o.propertyId = :propertyId "
            + "AND (:cropCycleId IS NULL OR o.cropCycleId = :cropCycleId) "
            + "AND (:fieldId IS NULL OR o.cropCycleId IN (SELECT c.id FROM CropCycle c WHERE c.fieldId = :fieldId)) "
            + "AND (:from IS NULL OR o.operationDate >= :from) "
            + "AND (:to IS NULL OR o.operationDate <= :to) "
            + "ORDER BY o.operationDate DESC, o.id DESC")
    Page<CropOperation> search(@Param("propertyId") Long propertyId, @Param("cropCycleId") Long cropCycleId,
            @Param("fieldId") Long fieldId, @Param("from") LocalDate from, @Param("to") LocalDate to,
            Pageable pageable);

    /** Todas as operações das safras informadas — para calcular indicadores em memória. */
    List<CropOperation> findByPropertyIdAndCropCycleIdIn(Long propertyId, List<Long> cropCycleIds);

    /** Para bloquear a exclusão de uma máquina já usada em operações (MachineService.delete). */
    boolean existsByMachineId(Long machineId);
}
