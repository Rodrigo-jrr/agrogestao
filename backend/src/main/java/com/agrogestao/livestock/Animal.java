package com.agrogestao.livestock;

import com.agrogestao.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "animals")
public class Animal extends BaseEntity {

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    /** Brinco/identificação; único entre os animais ativos da propriedade. */
    @Column(nullable = false, length = 30)
    private String tag;

    @Column(length = 100)
    private String name;

    /** FK para o catálogo de espécies da propriedade (ver {@link SpeciesCatalog}). */
    @Column(name = "species_id", nullable = false)
    private Long speciesId;

    @Column(length = 60)
    private String breed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Sex sex;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "birth_date_estimated", nullable = false)
    private boolean birthDateEstimated;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private AnimalOrigin origin;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "mother_id")
    private Long motherId;

    /** Pai: touro, reprodutor ou sêmen — texto livre. */
    @Column(length = 100)
    private String sire;

    @Column(name = "batch_id")
    private Long batchId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private AnimalStatus status = AnimalStatus.ATIVO;

    @Column(name = "exit_date")
    private LocalDate exitDate;

    @Column(name = "exit_reason", length = 255)
    private String exitReason;

    @Column(name = "current_weight", precision = 10, scale = 2)
    private BigDecimal currentWeight;

    @Column(length = 500)
    private String notes;

    public boolean isActive() {
        return status == AnimalStatus.ATIVO;
    }
}
