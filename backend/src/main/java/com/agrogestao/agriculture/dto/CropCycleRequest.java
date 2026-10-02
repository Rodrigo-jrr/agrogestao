package com.agrogestao.agriculture.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CropCycleRequest(
        @NotNull Long fieldId,
        @NotNull Long cropId,
        @Size(max = 100) String variety,
        @NotNull LocalDate plannedPlantingDate,
        LocalDate plannedHarvestDate) {
}
