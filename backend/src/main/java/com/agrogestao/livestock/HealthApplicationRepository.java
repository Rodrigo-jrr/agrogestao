package com.agrogestao.livestock;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HealthApplicationRepository extends JpaRepository<HealthApplication, Long> {

    Page<HealthApplication> findByPropertyIdOrderByApplicationDateDescIdDesc(Long propertyId, Pageable pageable);

    /** Próximos passos pendentes até {@code until} (inclui os atrasados). */
    @Query("SELECT h FROM HealthApplication h WHERE h.propertyId = :propertyId AND h.nextDate IS NOT NULL "
            + "AND h.followUpDoneAt IS NULL AND h.nextDate <= :until ORDER BY h.nextDate, h.id")
    List<HealthApplication> findPendingFollowUps(@Param("propertyId") Long propertyId, @Param("until") LocalDate until);

    boolean existsByBatchId(Long batchId);
}
