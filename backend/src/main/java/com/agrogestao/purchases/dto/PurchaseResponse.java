package com.agrogestao.purchases.dto;

import com.agrogestao.finance.PaymentType;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PurchaseResponse {

    private final Long id;
    private final Long supplierId;
    private final LocalDate purchaseDate;
    private final BigDecimal totalAmount;
    private final boolean canceled;
    private final PaymentType paymentType;
    /** 0 para à vista. */
    private final int installmentCount;
}
