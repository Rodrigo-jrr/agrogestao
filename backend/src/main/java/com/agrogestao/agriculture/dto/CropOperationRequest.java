package com.agrogestao.agriculture.dto;

import com.agrogestao.agriculture.CropOperationKind;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Operação agrícola numa safra. Com {@code productId}, o produto sai do estoque pela quantidade informada,
 * ao custo médio; sem ele, {@code productName} (se houver) e {@code cost} valem como digitados — a operação
 * pode não usar nenhum insumo (ex.: preparo do solo).
 */
public record CropOperationRequest(
        @NotNull Long cropCycleId,
        @NotNull CropOperationKind kind,
        @NotNull LocalDate operationDate,
        Long productId,
        @Size(max = 150) String productName,
        @DecimalMin(value = "0.001", message = "deve ser maior que zero")
        @Digits(integer = 9, fraction = 3, message = "acima do permitido ou com mais de 3 casas decimais")
        BigDecimal quantity,
        @Size(max = 20) String quantityUnit,
        @PositiveOrZero
        @Digits(integer = 13, fraction = 2, message = "acima do permitido ou com mais de 2 casas decimais")
        BigDecimal cost,
        @Size(max = 100) String responsible,
        @Size(max = 150) String machine,
        @Size(max = 500) String notes) {
}
