package com.agrogestao.agriculture.dto;

import com.agrogestao.agriculture.CropOperationKind;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CropOperationResponse(
        Long id,
        Long cropCycleId,
        Long fieldId,
        String fieldName,
        Long cropId,
        String cropName,
        CropOperationKind kind,
        LocalDate operationDate,
        Long productId,
        String productName,
        BigDecimal quantity,
        String quantityUnit,
        BigDecimal totalCost,
        String responsible,
        String machine,
        String notes) {
}
