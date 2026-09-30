package com.agrogestao.finance;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountReceivableRepository extends JpaRepository<AccountReceivable, Long> {

    List<AccountReceivable> findByPropertyIdOrderByDueDateAsc(Long propertyId);

    List<AccountReceivable> findByPropertyIdAndReceivedFalseAndDueDateLessThanEqual(Long propertyId, LocalDate to);

    boolean existsByFinancialTransactionId(Long financialTransactionId);

    List<AccountReceivable> findBySaleIdOrderByInstallmentNumberAsc(Long saleId);
}
