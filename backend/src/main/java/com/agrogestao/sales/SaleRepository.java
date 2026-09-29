package com.agrogestao.sales;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    List<Sale> findByPropertyIdOrderBySaleDateDesc(Long propertyId);

    boolean existsByFinancialTransactionId(Long financialTransactionId);
}
