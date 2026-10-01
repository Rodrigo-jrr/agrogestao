package com.agrogestao.livestock.dto;

import com.agrogestao.livestock.HealthKind;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Aplicação sanitária em animais com brinco ({@code animalIds}) ou num lote inteiro ({@code batchId}).
 * Com {@code productId}, o produto sai do estoque (dose × cabeças) pelo custo médio; sem ele, vale o nome
 * informado e o custo digitado. {@code followUpOfId}: esta aplicação é o reforço/continuação de outra.
 */
public record HealthApplicationRequest(
        @NotNull HealthKind kind,
        @NotNull LocalDate date,
        @Size(max = 500) List<Long> animalIds,
        Long batchId,
        Long productId,
        @Size(max = 150) String productName,
        @DecimalMin(value = "0.001", message = "deve ser maior que zero")
        @Digits(integer = 9, fraction = 3, message = "acima do permitido ou com mais de 3 casas decimais")
        BigDecimal dose,
        @Size(max = 20) String doseUnit,
        @PositiveOrZero
        @Digits(integer = 13, fraction = 2, message = "acima do permitido ou com mais de 2 casas decimais")
        BigDecimal cost,
        @PositiveOrZero @Max(365) Integer withdrawalDays,
        LocalDate nextDate,
        @Size(max = 255) String nextDescription,
        @Size(max = 100) String responsible,
        @Size(max = 500) String notes,
        Long followUpOfId) {
}
