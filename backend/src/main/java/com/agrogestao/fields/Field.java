package com.agrogestao.fields;

import com.agrogestao.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Talhão de uma propriedade. Cultura/safra e indicadores vêm nas próximas entregas da Agricultura. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "fields")
public class Field extends BaseEntity {

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "area_ha", nullable = false, precision = 12, scale = 2)
    private BigDecimal areaHa;

    @Column(length = 255)
    private String location;

    @Column(name = "soil_type", length = 100)
    private String soilType;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    public boolean isArchived() {
        return archivedAt != null;
    }
}
