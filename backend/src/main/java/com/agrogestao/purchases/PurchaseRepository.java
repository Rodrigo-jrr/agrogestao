package com.agrogestao.purchases;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    List<Purchase> findByPropertyIdOrderByPurchaseDateDesc(Long propertyId);

    @Query("SELECT x FROM Purchase x WHERE x.propertyId = :propertyId "
            + "AND (:partyId IS NULL OR x.supplierId = :partyId) "
            + "AND (:from IS NULL OR x.purchaseDate >= :from) AND (:to IS NULL OR x.purchaseDate <= :to) "
            + "ORDER BY x.purchaseDate DESC, x.id DESC")
    Page<Purchase> search(
            @Param("propertyId") Long propertyId,
            @Param("partyId") Long partyId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable);

    boolean existsByFinancialTransactionId(Long financialTransactionId);
}
