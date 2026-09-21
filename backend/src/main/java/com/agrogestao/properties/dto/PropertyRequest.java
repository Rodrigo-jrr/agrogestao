package com.agrogestao.properties.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PropertyRequest {

    @NotNull
    private Long organizationId;

    @NotBlank
    private String name;

    private String location;

    @PositiveOrZero
    private BigDecimal totalAreaHa;
}
