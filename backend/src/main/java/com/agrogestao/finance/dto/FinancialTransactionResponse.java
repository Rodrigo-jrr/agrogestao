package com.agrogestao.finance.dto;

import com.agrogestao.finance.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FinancialTransactionResponse {

    private final Long id;
    private final TransactionType type;
    private final String description;
    private final BigDecimal amount;
    private final LocalDate transactionDate;
    private final Long costCenterId;
}
