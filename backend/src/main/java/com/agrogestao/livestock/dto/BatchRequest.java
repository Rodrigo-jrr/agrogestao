package com.agrogestao.livestock.dto;

import com.agrogestao.livestock.BatchTracking;
import com.agrogestao.livestock.Species;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Espécie e tipo de controle não mudam depois que o lote tem animais ou movimentações. */
public record BatchRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull Species species,
        @NotNull BatchTracking tracking,
        Long activityId,
        @Size(max = 500) String notes) {
}
