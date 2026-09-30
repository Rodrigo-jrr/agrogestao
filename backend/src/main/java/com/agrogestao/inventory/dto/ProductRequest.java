package com.agrogestao.inventory.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductRequest {

    @NotBlank
    @Size(max = 150)
    private String name;

    @Size(max = 60)
    private String category;

    @NotBlank
    @Size(max = 20)
    private String unit;

    @NotNull
    @PositiveOrZero
    @Digits(integer = 12, fraction = 3, message = "quantidade acima do permitido ou com mais de 3 casas decimais")
    private BigDecimal minStock;
}
