package com.agrogestao.dashboard.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProductionSummaryItem {

    private final String productName;
    private final BigDecimal totalQuantity;
    private final String unit;
}
