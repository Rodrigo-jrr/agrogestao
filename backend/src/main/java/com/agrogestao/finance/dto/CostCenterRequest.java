package com.agrogestao.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CostCenterRequest {

    @NotBlank
    @Size(max = 150)
    private String name;

    private Long activityId;
}
