package com.agrogestao.finance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CostCenterRequest {

    @NotBlank
    private String name;

    private Long activityId;
}
