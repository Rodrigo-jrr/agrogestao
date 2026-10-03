package com.agrogestao.agriculture.dto;

import com.agrogestao.agriculture.CropHarvestDestination;
import com.agrogestao.finance.PaymentType;
import com.agrogestao.finance.dto.InstallmentInput;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Colheita de uma safra. Com {@code destination=ESTOQUE}, exige {@code productId} (produto já cadastrado)
 * e {@code unitCost}: entra no estoque como uma entrada de compra, ao custo informado. Com
 * {@code destination=VENDA}, exige {@code customerId} e {@code totalAmount}: vira receita direto (à vista
 * ou a prazo), sem depender de produto ou estoque — a colheita não passa pelo módulo Vendas.
 */
public record CropHarvestRequest(
        @NotNull Long cropCycleId,
        @NotNull LocalDate harvestDate,
        @NotNull
        @DecimalMin(value = "0.001", message = "deve ser maior que zero")
        @Digits(integer = 9, fraction = 3, message = "acima do permitido ou com mais de 3 casas decimais")
        BigDecimal quantity,
        @NotBlank @Size(max = 20) String quantityUnit,
        @NotNull CropHarvestDestination destination,
        Long productId,
        @PositiveOrZero
        @Digits(integer = 13, fraction = 2, message = "acima do permitido ou com mais de 2 casas decimais")
        BigDecimal unitCost,
        Long customerId,
        @PositiveOrZero
        @Digits(integer = 13, fraction = 2, message = "acima do permitido ou com mais de 2 casas decimais")
        BigDecimal totalAmount,
        PaymentType paymentType,
        @Valid @Size(max = 48) List<InstallmentInput> installments) {
}
