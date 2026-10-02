package com.agrogestao.agriculture;

import com.agrogestao.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Cultura cadastrada livremente por uma propriedade — mesmo padrão do catálogo de espécies. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "crops")
public class Crop extends BaseEntity {

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    public boolean isArchived() {
        return archivedAt != null;
    }
}
