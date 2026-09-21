package com.agrogestao.purchases.dto;

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
}
