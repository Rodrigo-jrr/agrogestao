package com.agrogestao.machinery;

import com.agrogestao.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Máquina ou implemento de uma propriedade. {@code currentReading} guarda a leitura atual (horas ou km,
 * conforme {@code measurementType}) — atualizada manualmente por agora; a Entrega 2 (operações agrícolas)
 * passa a atualizá-la automaticamente pelo uso.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "machines")
public class Machine extends BaseEntity {

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "machine_type_id", nullable = false)
    private Long machineTypeId;

    @Column(length = 60)
    private String identification;

    @Enumerated(EnumType.STRING)
    @Column(name = "measurement_type", nullable = false, length = 20)
    private MeasurementType measurementType;

    @Column(name = "current_reading", nullable = false, precision = 12, scale = 2)
    private BigDecimal currentReading = BigDecimal.ZERO;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    public boolean isArchived() {
        return archivedAt != null;
    }
}
