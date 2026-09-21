package com.agrogestao.activities.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActivityRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String type;

    private BigDecimal areaHa;

    private String responsible;
}
