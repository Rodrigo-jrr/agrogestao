package com.agrogestao.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AccountReceivableResponse {

    private final Long id;
    private final String description;
    private final BigDecimal amount;
    private final LocalDate dueDate;
    private final Long customerId;
    private final boolean received;
    private final Long saleId;
    private final Integer installmentNumber;
    private final Integer installmentCount;
}
