package com.agrogestao.purchases.dto;

import com.agrogestao.finance.PaymentType;
import com.agrogestao.finance.dto.InstallmentView;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PurchaseDetailResponse(
        Long id,
        Long supplierId,
        String supplierName,
        LocalDate purchaseDate,
        BigDecimal totalAmount,
        PaymentType paymentType,
        boolean canceled,
        List<Item> items,
        List<InstallmentView> installments) {

    public record Item(Long productId, String productName, String unit, BigDecimal quantity, BigDecimal unitPrice,
            BigDecimal total) {
    }
}
