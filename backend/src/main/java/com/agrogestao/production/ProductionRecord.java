package com.agrogestao.production;

import com.agrogestao.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "production_records")
public class ProductionRecord extends BaseEntity {

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "activity_id")
    private Long activityId;

    @Column(name = "product_name", nullable = false, length = 150)
    private String productName;

    @Column(nullable = false, precision = 15, scale = 3)
    private BigDecimal quantity;

    @Column(nullable = false, length = 20)
    private String unit;

    @Column(name = "production_date", nullable = false)
    private LocalDate productionDate;
}
