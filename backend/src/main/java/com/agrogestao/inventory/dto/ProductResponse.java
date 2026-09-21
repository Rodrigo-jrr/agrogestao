package com.agrogestao.inventory.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProductResponse {

    private final Long id;
    private final String name;
    private final String category;
    private final String unit;
    private final BigDecimal minStock;
    private final BigDecimal currentQuantity;
}
