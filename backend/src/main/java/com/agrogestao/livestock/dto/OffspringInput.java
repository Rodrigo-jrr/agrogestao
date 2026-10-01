package com.agrogestao.livestock.dto;

import com.agrogestao.livestock.Sex;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Cria nascida num parto: entra no rebanho com a data do parto como nascimento, pela mãe já identificada. */
public record OffspringInput(
        @NotBlank @Size(max = 30) String tag,
        @Size(max = 100) String name,
        @NotNull Sex sex,
        @Size(max = 60) String breed,
        @DecimalMin(value = "0.01", message = "deve ser maior que zero")
        @Digits(integer = 6, fraction = 2, message = "acima do permitido ou com mais de 2 casas decimais")
        BigDecimal weight) {
}
