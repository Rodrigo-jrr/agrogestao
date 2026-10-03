package com.agrogestao.machinery.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record MachineReadingRequest(
        @NotNull
        @DecimalMin(value = "0", message = "não pode ser negativa")
        @Digits(integer = 10, fraction = 2, message = "acima do permitido ou com mais de 2 casas decimais")
        BigDecimal reading) {
}
