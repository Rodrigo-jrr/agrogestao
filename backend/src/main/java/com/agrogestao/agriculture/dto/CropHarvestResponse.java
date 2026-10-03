package com.agrogestao.agriculture.dto;

import com.agrogestao.agriculture.CropHarvestDestination;
import com.agrogestao.finance.PaymentType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CropHarvestResponse(
        Long id,
        Long cropCycleId,
        Long fieldId,
        String fieldName,
        Long cropId,
        String cropName,
        LocalDate harvestDate,
        BigDecimal quantity,
        String quantityUnit,
        CropHarvestDestination destination,
        Long productId,
        String productName,
        BigDecimal unitCost,
        Long customerId,
        String customerName,
        BigDecimal totalAmount,
        PaymentType paymentType) {
}
