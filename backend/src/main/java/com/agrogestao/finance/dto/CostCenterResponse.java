package com.agrogestao.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CostCenterResponse {

    private final Long id;
    private final String name;
    private final Long activityId;
}
