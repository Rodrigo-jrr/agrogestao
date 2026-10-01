package com.agrogestao.fields.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record FieldRequest(
        @NotBlank @Size(max = 150) String name,
        @NotNull
        @DecimalMin(value = "0.01", message = "deve ser maior que zero")
        @Digits(integer = 10, fraction = 2, message = "acima do permitido ou com mais de 2 casas decimais")
        BigDecimal areaHa,
        @Size(max = 255) String location,
        @Size(max = 100) String soilType) {
}
