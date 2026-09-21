package com.agrogestao.activities;

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
@Table(name = "activities")
public class Activity extends BaseEntity {

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 60)
    private String type;

    @Column(name = "area_ha")
    private BigDecimal areaHa;

    @Column(length = 150)
    private String responsible;
}
