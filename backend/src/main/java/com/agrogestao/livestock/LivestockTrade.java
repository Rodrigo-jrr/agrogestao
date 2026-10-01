package com.agrogestao.livestock;

import com.agrogestao.common.BaseEntity;
import com.agrogestao.finance.PaymentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Compra ou venda de animais (individuais ou por contagem de um lote). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "livestock_trades")
public class LivestockTrade extends BaseEntity {

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TradeKind kind;

    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "customer_id")
    private Long customerId;

    /** Lote por contagem movimentado (compra/venda sem animais individuais). */
    @Column(name = "batch_id")
    private Long batchId;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(name = "head_count", nullable = false)
    private int headCount;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 20)
    private PaymentType paymentType = PaymentType.CASH;

    @Column(name = "installment_count", nullable = false)
    private int installmentCount;

    @Column(name = "financial_transaction_id")
    private Long financialTransactionId;

    @Column(length = 500)
    private String notes;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    public boolean isCanceled() {
        return canceledAt != null;
    }
}
