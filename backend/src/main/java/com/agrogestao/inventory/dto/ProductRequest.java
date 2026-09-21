package com.agrogestao.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductRequest {

    @NotBlank
    private String name;

    private String category;

    @NotBlank
    private String unit;

    @NotNull
    private BigDecimal minStock;
}
