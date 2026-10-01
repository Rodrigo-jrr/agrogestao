package com.agrogestao.livestock.dto;

import com.agrogestao.livestock.AnimalEventType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record AnimalEventResponse(Long id, AnimalEventType type, LocalDate date, Long animalId, String animalTag,
        Long batchId, String batchName, Integer headCount, BigDecimal weight, String fromBatchName,
        String toBatchName, Long tradeId, BigDecimal amount, String description, Long applicationId,
        LocalDate withdrawalUntil) {
}
