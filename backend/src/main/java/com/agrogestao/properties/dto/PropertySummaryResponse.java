package com.agrogestao.properties.dto;

import com.agrogestao.properties.PropertyRole;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PropertySummaryResponse {

    private final Long id;
    private final Long organizationId;
    private final String name;
    private final String location;
    private final BigDecimal totalAreaHa;
    private final PropertyRole role;
}
