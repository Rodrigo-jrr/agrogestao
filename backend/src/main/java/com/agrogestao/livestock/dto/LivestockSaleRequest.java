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

/** Venda de animais: ou animais com brinco ({@code animalIds}), ou cabeças de um lote por contagem. */
public record LivestockSaleRequest(
        @NotNull Long customerId,
        @NotNull LocalDate tradeDate,
        @NotNull @Positive
        @Digits(integer = 13, fraction = 2, message = "acima do permitido ou com mais de 2 casas decimais")
        BigDecimal totalAmount,
        PaymentType paymentType,
        @Valid @Size(max = 48) List<InstallmentInput> installments,
        @Size(max = 500) List<Long> animalIds,
        Long batchId,
        @Positive @Max(1_000_000) Integer headCount,
        @Size(max = 500) String notes) {
}
