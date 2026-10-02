package com.agrogestao.agriculture;

import com.agrogestao.common.BaseEntity;
import com.agrogestao.finance.PaymentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Colheita de uma safra: vai para o estoque (produto já cadastrado, ao custo informado) ou vira receita
 * direto, como venda — sem passar pelo módulo Vendas, mesmo padrão de {@code livestock_trades}. Uma safra
 * pode ter várias colheitas.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "crop_harvests")
public class CropHarvest extends BaseEntity {

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "crop_cycle_id", nullable = false)
    private Long cropCycleId;

    @Column(name = "harvest_date", nullable = false)
    private LocalDate harvestDate;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(name = "quantity_unit", nullable = false, length = 20)
    private String quantityUnit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CropHarvestDestination destination;

    // ESTOQUE
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "unit_cost", precision = 15, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "inventory_movement_id")
    private Long inventoryMovementId;

    // VENDA
    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", length = 20)
    private PaymentType paymentType;

    @Column(name = "financial_transaction_id")
    private Long financialTransactionId;
}
