package com.agrogestao.properties;

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
@Table(name = "properties")
public class Property extends BaseEntity {

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 255)
    private String location;

    @Column(name = "total_area_ha")
    private BigDecimal totalAreaHa;
}
