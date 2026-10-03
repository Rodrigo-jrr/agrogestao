package com.agrogestao.agriculture;

import com.agrogestao.common.BaseEntity;
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
 * Operação agrícola pontual numa safra (preparo, plantio, adubação, pulverização). Um insumo do estoque
 * (ou só o nome) opcional, com custo, responsável e máquina usada (texto livre).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "crop_operations")
public class CropOperation extends BaseEntity {

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "crop_cycle_id", nullable = false)
    private Long cropCycleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CropOperationKind kind;

    @Column(name = "operation_date", nullable = false)
    private LocalDate operationDate;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_name", length = 150)
    private String productName;

    @Column(precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(name = "quantity_unit", length = 20)
    private String quantityUnit;

    @Column(name = "total_cost", precision = 15, scale = 2)
    private BigDecimal totalCost;

    @Column(length = 100)
    private String responsible;

    @Column(length = 150)
    private String machine;

    @Column(name = "machine_id")
    private Long machineId;

    @Column(length = 500)
    private String notes;

    @Column(name = "inventory_movement_id")
    private Long inventoryMovementId;
}
