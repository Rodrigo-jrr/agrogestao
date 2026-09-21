package com.agrogestao.purchases;

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
@Table(name = "suppliers")
public class Supplier extends BaseEntity {

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 30)
    private String document;

    @Column(length = 30)
    private String phone;

    @Column(length = 180)
    private String email;
}
