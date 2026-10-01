package com.agrogestao.livestock;

import com.agrogestao.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "animal_batches")
public class AnimalBatch extends BaseEntity {

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(nullable = false, length = 100)
    private String name;

    /** FK para o catálogo de espécies da propriedade (ver {@link SpeciesCatalog}). */
    @Column(name = "species_id", nullable = false)
    private Long speciesId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private BatchTracking tracking;

    /** Só para lotes AGGREGATE; nos individuais a contagem vem dos animais ativos. */
    @Column(name = "head_count", nullable = false)
    private int headCount;

    @Column(name = "activity_id")
    private Long activityId;

    @Column(length = 500)
    private String notes;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    public boolean isArchived() {
        return archivedAt != null;
    }

    public boolean isAggregate() {
        return tracking == BatchTracking.AGGREGATE;
    }
}
