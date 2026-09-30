package com.agrogestao.activities.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActivityRequest {

    @NotBlank
    @Size(max = 150)
    private String name;

    @NotBlank
    @Size(max = 60)
    private String type;

    @PositiveOrZero
    @Digits(integer = 10, fraction = 2, message = "área acima do permitido ou com mais de 2 casas decimais")
    private BigDecimal areaHa;

    @Size(max = 150)
    private String responsible;
}
