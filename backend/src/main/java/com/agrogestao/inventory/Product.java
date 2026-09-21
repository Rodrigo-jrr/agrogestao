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
@Table(name = "products")
public class Product extends BaseEntity {

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 60)
    private String category;

    @Column(nullable = false, length = 20)
    private String unit;

    @Column(name = "min_stock", nullable = false, precision = 15, scale = 3)
    private BigDecimal minStock = BigDecimal.ZERO;
}
