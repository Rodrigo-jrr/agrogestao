package com.agrogestao.properties.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record PropertyUpdateRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 255) String location,
        @PositiveOrZero
        @Digits(integer = 10, fraction = 2, message = "área acima do permitido ou com mais de 2 casas decimais")
        BigDecimal totalAreaHa) {
}
