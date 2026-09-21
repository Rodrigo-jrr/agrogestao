package com.agrogestao.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FinancialTransactionRepository extends JpaRepository<FinancialTransaction, Long> {

    List<FinancialTransaction> findByPropertyIdAndTransactionDateBetweenOrderByTransactionDateDesc(
            Long propertyId, LocalDate from, LocalDate to);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM FinancialTransaction t "
            + "WHERE t.propertyId = :propertyId AND t.type = :type "
            + "AND t.transactionDate BETWEEN :from AND :to")
    BigDecimal sumByPropertyIdAndTypeAndPeriod(
            @Param("propertyId") Long propertyId,
            @Param("type") TransactionType type,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
