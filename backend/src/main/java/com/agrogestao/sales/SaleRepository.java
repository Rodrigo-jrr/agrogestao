package com.agrogestao.sales;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    List<Sale> findByPropertyIdOrderBySaleDateDesc(Long propertyId);

    @Query("SELECT x FROM Sale x WHERE x.propertyId = :propertyId "
            + "AND (:partyId IS NULL OR x.customerId = :partyId) "
            + "AND (:from IS NULL OR x.saleDate >= :from) AND (:to IS NULL OR x.saleDate <= :to) "
            + "ORDER BY x.saleDate DESC, x.id DESC")
    Page<Sale> search(
            @Param("propertyId") Long propertyId,
            @Param("partyId") Long partyId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable);

    boolean existsByFinancialTransactionId(Long financialTransactionId);
}
