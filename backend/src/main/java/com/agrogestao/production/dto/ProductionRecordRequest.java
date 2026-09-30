package com.agrogestao.production.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductionRecordRequest {

    @NotBlank
    @Size(max = 150)
    private String productName;

    @NotNull
    @Positive
    @Digits(integer = 12, fraction = 3, message = "quantidade acima do permitido ou com mais de 3 casas decimais")
    private BigDecimal quantity;

    @NotBlank
    @Size(max = 20)
    private String unit;

    @NotNull
    private LocalDate productionDate;

    private Long activityId;
}
