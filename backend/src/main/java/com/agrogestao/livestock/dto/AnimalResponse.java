package com.agrogestao.livestock.dto;

import com.agrogestao.livestock.AnimalOrigin;
import com.agrogestao.livestock.AnimalStatus;
import com.agrogestao.livestock.Sex;
import java.math.BigDecimal;
import java.time.LocalDate;

public record AnimalResponse(Long id, String tag, String name, Long speciesId, String speciesName, String breed,
        Sex sex, LocalDate birthDate, boolean birthDateEstimated, AnimalOrigin origin, LocalDate entryDate,
        Long motherId, String motherTag, String sire, Long batchId, String batchName, AnimalStatus status,
        LocalDate exitDate, String exitReason, BigDecimal currentWeight, String notes, LocalDate withdrawalUntil) {
}
