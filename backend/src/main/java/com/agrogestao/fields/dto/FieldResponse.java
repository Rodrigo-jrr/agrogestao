package com.agrogestao.fields.dto;

import java.math.BigDecimal;

public record FieldResponse(Long id, String name, BigDecimal areaHa, String location, String soilType, boolean archived) {
}
