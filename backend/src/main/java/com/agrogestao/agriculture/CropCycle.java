package com.agrogestao.agriculture;

import com.agrogestao.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Safra/ciclo de cultivo: talhão + cultura (referências reais), variedade em texto livre. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "crop_cycles")
public class CropCycle extends BaseEntity {

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "field_id", nullable = false)
    private Long fieldId;

    @Column(name = "crop_id", nullable = false)
    private Long cropId;

    @Column(length = 100)
    private String variety;

    @Column(name = "planned_planting_date", nullable = false)
    private LocalDate plannedPlantingDate;

    @Column(name = "planned_harvest_date")
    private LocalDate plannedHarvestDate;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    public boolean isArchived() {
        return archivedAt != null;
    }
}
