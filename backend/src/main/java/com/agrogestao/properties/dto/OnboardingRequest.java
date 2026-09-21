package com.agrogestao.properties.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OnboardingRequest {

    @NotBlank
    private String organizationName;

    @NotBlank
    private String propertyName;

    private String location;

    private BigDecimal totalAreaHa;

    @NotEmpty(message = "selecione ao menos uma atividade")
    @Valid
    private List<ActivityInput> activities;

    @Getter
    @Setter
    public static class ActivityInput {

        @NotBlank
        private String name;

        @NotBlank
        private String type;

        private BigDecimal areaHa;
    }
}
