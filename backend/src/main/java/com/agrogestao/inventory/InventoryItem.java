package com.agrogestao.inventory;

import com.agrogestao.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "inventory_items")
public class InventoryItem extends BaseEntity {

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false, precision = 15, scale = 3)
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(name = "average_cost", nullable = false, precision = 15, scale = 2)
    private BigDecimal averageCost = BigDecimal.ZERO;
}
