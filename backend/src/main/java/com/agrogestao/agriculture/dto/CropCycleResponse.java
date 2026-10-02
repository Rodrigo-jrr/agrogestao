package com.agrogestao.agriculture.dto;

import java.time.LocalDate;

public record CropCycleResponse(
        Long id,
        Long fieldId,
        String fieldName,
        Long cropId,
        String cropName,
        String variety,
        LocalDate plannedPlantingDate,
        LocalDate plannedHarvestDate,
        boolean archived) {
}
