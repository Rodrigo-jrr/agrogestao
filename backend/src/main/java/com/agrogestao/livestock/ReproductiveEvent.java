package com.agrogestao.livestock;

import com.agrogestao.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Evento reprodutivo de uma fêmea (cio, cobertura, inseminação, diagnóstico de prenhez, parto ou aborto).
 * Cada um gera um AnimalEventType.REPRODUCAO na linha do tempo do animal, ligado por
 * {@code animal_events.reproductive_event_id} — mesmo desenho de {@link HealthApplication}.
 * Situação reprodutiva e histórico são derivados desta linha do tempo, não guardados à parte.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "reproductive_events")
public class ReproductiveEvent extends BaseEntity {

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "animal_id", nullable = false)
    private Long animalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReproductiveEventKind kind;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    /** Touro/reprodutor (cobertura) ou identificação do sêmen (inseminação) — texto livre, como animals.sire. */
    @Column(length = 100)
    private String sire;

    @Enumerated(EnumType.STRING)
    @Column(name = "pregnancy_result", length = 10)
    private PregnancyResult pregnancyResult;

    @Column(name = "expected_calving_date")
    private LocalDate expectedCalvingDate;

    /** No parto/aborto, o diagnóstico de prenhez (positivo) que esta gestação encerra. */
    @Column(name = "diagnosis_event_id")
    private Long diagnosisEventId;

    /**
     * Preenchido no próprio diagnóstico quando a gestação termina sem parto/aborto — o animal saiu do
     * rebanho (venda, morte, descarte) antes disso. A saída já é o evento auditado; isto só fecha a gestação.
     */
    @Column(name = "closed_without_birth_at")
    private LocalDateTime closedWithoutBirthAt;

    /** No parto: quantas crias nasceram (os cadastros das crias ficam em Animal). */
    @Column(name = "offspring_count")
    private Integer offspringCount;

    @Column(length = 500)
    private String notes;
}
