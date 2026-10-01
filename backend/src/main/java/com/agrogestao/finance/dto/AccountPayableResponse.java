package com.agrogestao.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AccountPayableResponse {

    private final Long id;
    private final String description;
    private final BigDecimal amount;
    private final LocalDate dueDate;
    private final Long supplierId;
    private final boolean paid;
    private final Long purchaseId;
    private final Long livestockTradeId;
    private final Integer installmentNumber;
    private final Integer installmentCount;
}
