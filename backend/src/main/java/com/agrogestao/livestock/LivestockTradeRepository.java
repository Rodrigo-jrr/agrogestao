package com.agrogestao.livestock;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LivestockTradeRepository extends JpaRepository<LivestockTrade, Long> {

    boolean existsByFinancialTransactionId(Long financialTransactionId);

    boolean existsByBatchId(Long batchId);

    @Query("SELECT t FROM LivestockTrade t WHERE t.propertyId = :propertyId AND (:kind IS NULL OR t.kind = :kind) "
            + "ORDER BY t.tradeDate DESC, t.id DESC")
    Page<LivestockTrade> search(@Param("propertyId") Long propertyId, @Param("kind") TradeKind kind, Pageable pageable);
}
