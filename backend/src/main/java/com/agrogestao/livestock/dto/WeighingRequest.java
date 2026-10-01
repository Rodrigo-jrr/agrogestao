package com.agrogestao.livestock.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Pesagem: ou o peso de cada animal ({@code entries}), ou o peso médio de uma amostra de um lote por
 * contagem ({@code batchId} + {@code averageWeight} + {@code sampleSize}).
 */
public record WeighingRequest(
        @NotNull LocalDate date,
        @Valid @Size(max = 500) List<Entry> entries,
        Long batchId,
        @DecimalMin(value = "0.001", message = "deve ser maior que zero")
        @Digits(integer = 6, fraction = 3, message = "acima do permitido ou com mais de 3 casas decimais")
        BigDecimal averageWeight,
        @Positive @Max(1_000_000) Integer sampleSize) {

    public record Entry(
            @NotNull Long animalId,
            @NotNull @DecimalMin(value = "0.01", message = "deve ser maior que zero")
            @Digits(integer = 6, fraction = 2, message = "acima do permitido ou com mais de 2 casas decimais")
            BigDecimal weight) {
    }
}
