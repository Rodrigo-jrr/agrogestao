package com.agrogestao.livestock;

import com.agrogestao.common.BaseEntity;
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

/**
 * Manejo sanitário: um produto (do estoque ou só o nome) aplicado num animal, em vários ou num lote.
 * Guarda a carência e o próximo passo (reforço, fim do tratamento), que vira alerta.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "health_applications")
public class HealthApplication extends BaseEntity {

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HealthKind kind;

    @Column(name = "application_date", nullable = false)
    private LocalDate applicationDate;

    /** Lote inteiro (quando a aplicação foi no lote). */
    @Column(name = "batch_id")
    private Long batchId;

    @Column(name = "head_count", nullable = false)
    private int headCount;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_name", nullable = false, length = 150)
    private String productName;

    /** Dose por cabeça, na unidade do produto. */
    @Column(precision = 12, scale = 3)
    private BigDecimal dose;

    @Column(name = "dose_unit", length = 20)
    private String doseUnit;

    @Column(name = "total_quantity", precision = 12, scale = 3)
    private BigDecimal totalQuantity;

    @Column(name = "total_cost", precision = 15, scale = 2)
    private BigDecimal totalCost;

    @Column(name = "withdrawal_days")
    private Integer withdrawalDays;

    @Column(name = "withdrawal_until")
    private LocalDate withdrawalUntil;

    /** Reforço, nova dose ou fim do tratamento. */
    @Column(name = "next_date")
    private LocalDate nextDate;

    @Column(name = "next_description", length = 255)
    private String nextDescription;

    @Column(name = "follow_up_done_at")
    private LocalDateTime followUpDoneAt;

    @Column(length = 100)
    private String responsible;

    @Column(length = 500)
    private String notes;

    @Column(name = "inventory_movement_id")
    private Long inventoryMovementId;
}
