package com.agrogestao.activities.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ActivityResponse {

    private final Long id;
    private final String name;
    private final String type;
    private final BigDecimal areaHa;
    private final String responsible;
}
