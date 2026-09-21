package com.agrogestao.production.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductionRecordRequest {

    @NotBlank
    private String productName;

    @NotNull
    @Positive
    private BigDecimal quantity;

    @NotBlank
    private String unit;

    @NotNull
    private LocalDate productionDate;

    private Long activityId;
}
