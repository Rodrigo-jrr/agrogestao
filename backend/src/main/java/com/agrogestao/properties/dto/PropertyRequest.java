package com.agrogestao.properties.dto;

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
public class PropertyRequest {

    @NotNull
    private Long organizationId;

    @NotBlank
    @Size(max = 150)
    private String name;

    @Size(max = 255)
    private String location;

    @PositiveOrZero
    @Digits(integer = 10, fraction = 2, message = "área acima do permitido ou com mais de 2 casas decimais")
    private BigDecimal totalAreaHa;
}
