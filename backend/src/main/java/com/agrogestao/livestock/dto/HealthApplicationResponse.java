package com.agrogestao.livestock.dto;

import com.agrogestao.livestock.HealthKind;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** {@code target}: "lote Vacas", "animal 184" ou "3 animais" — para listas e alertas. */
public record HealthApplicationResponse(Long id, HealthKind kind, LocalDate date, Long batchId, String batchName,
        int headCount, List<AnimalRef> animals, String target, Long productId, String productName, BigDecimal dose,
        String doseUnit, BigDecimal totalQuantity, BigDecimal totalCost, Integer withdrawalDays,
        LocalDate withdrawalUntil, LocalDate nextDate, String nextDescription, boolean followUpDone,
        String responsible, String notes) {

    public record AnimalRef(Long id, String tag) {
    }
}
