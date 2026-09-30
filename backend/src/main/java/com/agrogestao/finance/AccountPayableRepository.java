package com.agrogestao.finance;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountPayableRepository extends JpaRepository<AccountPayable, Long> {

    List<AccountPayable> findByPropertyIdOrderByDueDateAsc(Long propertyId);

    List<AccountPayable> findByPropertyIdAndPaidFalseAndDueDateLessThanEqual(Long propertyId, LocalDate to);

    boolean existsByFinancialTransactionId(Long financialTransactionId);

    List<AccountPayable> findByPurchaseIdOrderByInstallmentNumberAsc(Long purchaseId);
}
