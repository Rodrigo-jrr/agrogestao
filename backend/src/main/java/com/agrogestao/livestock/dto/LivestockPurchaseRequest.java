package com.agrogestao.livestock.dto;

import com.agrogestao.finance.PaymentType;
import com.agrogestao.finance.dto.InstallmentInput;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Compra de animais: ou uma lista de animais com brinco ({@code animals}), ou só a quantidade de cabeças
 * num lote por contagem ({@code batchId} + {@code headCount}).
 */
public record LivestockPurchaseRequest(
        @NotNull Long supplierId,
        @NotNull LocalDate tradeDate,
        @NotNull @Positive
        @Digits(integer = 13, fraction = 2, message = "acima do permitido ou com mais de 2 casas decimais")
        BigDecimal totalAmount,
        PaymentType paymentType,
        @Valid @Size(max = 48) List<InstallmentInput> installments,
        Long batchId,
        Long speciesId,
        @Valid @Size(max = 500) List<NewAnimalInput> animals,
        @Positive @Max(1_000_000) Integer headCount,
        @Size(max = 500) String notes) {
}
