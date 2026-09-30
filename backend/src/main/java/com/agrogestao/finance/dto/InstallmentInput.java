package com.agrogestao.finance.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record InstallmentInput(
        @NotNull LocalDate dueDate,
        @NotNull @Positive
        @Digits(integer = 13, fraction = 2, message = "valor acima do permitido ou com mais de 2 casas decimais")
        BigDecimal amount) {
}
