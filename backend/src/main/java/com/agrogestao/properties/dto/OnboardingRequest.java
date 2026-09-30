package com.agrogestao.properties.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OnboardingRequest {

    @NotBlank
    @Size(max = 150)
    private String organizationName;

    @NotBlank
    @Size(max = 150)
    private String propertyName;

    @Size(max = 255)
    private String location;

    @PositiveOrZero
    @Digits(integer = 10, fraction = 2, message = "área acima do permitido ou com mais de 2 casas decimais")
    private BigDecimal totalAreaHa;

    @NotEmpty(message = "selecione ao menos uma atividade")
    @Valid
    private List<ActivityInput> activities;

    @Getter
    @Setter
    public static class ActivityInput {

        @NotBlank
        @Size(max = 150)
        private String name;

        @NotBlank
        @Size(max = 60)
        private String type;

        @PositiveOrZero
        @Digits(integer = 10, fraction = 2, message = "área acima do permitido ou com mais de 2 casas decimais")
        private BigDecimal areaHa;
    }
}
