package com.agrogestao.production.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProductionRecordResponse {

    private final Long id;
    private final String productName;
    private final BigDecimal quantity;
    private final String unit;
    private final LocalDate productionDate;
    private final Long activityId;
}
