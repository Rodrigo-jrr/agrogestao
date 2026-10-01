package com.agrogestao.livestock;

import com.agrogestao.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Espécie cadastrada livremente por uma propriedade (nome e se admite brinco/cadastro individual). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "species")
public class SpeciesCatalog extends BaseEntity {

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(nullable = false, length = 60)
    private String name;

    /** Admite cadastro individual (com brinco) ou só contagem por lote. */
    @Column(name = "allows_individual", nullable = false)
    private boolean allowsIndividual = true;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    public boolean isArchived() {
        return archivedAt != null;
    }
}
