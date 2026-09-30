package com.agrogestao.finance;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountPayableRepository extends JpaRepository<AccountPayable, Long> {

    List<AccountPayable> findByPropertyIdOrderByDueDateAsc(Long propertyId);

    @Query("SELECT a FROM AccountPayable a WHERE a.propertyId = :propertyId "
            + "AND (:settled IS NULL OR a.paid = :settled) "
            + "AND (:q IS NULL OR LOWER(a.description) LIKE :q ESCAPE '\\') "
            + "ORDER BY a.dueDate ASC, a.id ASC")
    Page<AccountPayable> search(
            @Param("propertyId") Long propertyId,
            @Param("settled") Boolean settled,
            @Param("q") String q,
            Pageable pageable);

    List<AccountPayable> findByPropertyIdAndPaidFalseAndDueDateLessThanEqual(Long propertyId, LocalDate to);

    boolean existsByFinancialTransactionId(Long financialTransactionId);

    List<AccountPayable> findByPurchaseIdOrderByInstallmentNumberAsc(Long purchaseId);
}
