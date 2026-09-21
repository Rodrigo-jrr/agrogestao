package com.agrogestao.inventory;

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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "inventory_movements")
public class InventoryMovement extends BaseEntity {

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MovementType type;

    @Column(nullable = false, precision = 15, scale = 3)
    private BigDecimal quantity;

    @Column(name = "unit_cost", precision = 15, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "movement_date", nullable = false)
    private LocalDate movementDate;

    @Column(length = 255)
    private String notes;
}
