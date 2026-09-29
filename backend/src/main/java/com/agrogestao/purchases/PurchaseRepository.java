package com.agrogestao.purchases;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    List<Purchase> findByPropertyIdOrderByPurchaseDateDesc(Long propertyId);

    boolean existsByFinancialTransactionId(Long financialTransactionId);
}
