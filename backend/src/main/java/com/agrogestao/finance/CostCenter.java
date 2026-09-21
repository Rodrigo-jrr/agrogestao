package com.agrogestao.finance;

import com.agrogestao.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "cost_centers")
public class CostCenter extends BaseEntity {

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "activity_id")
    private Long activityId;

    @Column(nullable = false, length = 150)
    private String name;
}
