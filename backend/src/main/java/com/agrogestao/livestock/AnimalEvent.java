package com.agrogestao.livestock;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Um acontecimento na vida de um animal (animalId) ou de um lote inteiro (batchId + headCount). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "animal_events")
public class AnimalEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "animal_id")
    private Long animalId;

    @Column(name = "batch_id")
    private Long batchId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AnimalEventType type;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "head_count")
    private Integer headCount;

    @Column(precision = 10, scale = 2)
    private BigDecimal weight;

    @Column(name = "from_batch_id")
    private Long fromBatchId;

    @Column(name = "to_batch_id")
    private Long toBatchId;

    @Column(name = "trade_id")
    private Long tradeId;

    @Column(precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(length = 255)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
