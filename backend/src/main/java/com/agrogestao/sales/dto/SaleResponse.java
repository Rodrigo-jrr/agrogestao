package com.agrogestao.sales.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SaleResponse {

    private final Long id;
    private final Long customerId;
    private final LocalDate saleDate;
    private final BigDecimal totalAmount;
    private final boolean canceled;
}
