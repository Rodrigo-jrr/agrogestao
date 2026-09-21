package com.agrogestao.finance.dto;

import com.agrogestao.finance.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FinancialTransactionRequest {

    @NotNull
    private TransactionType type;

    @NotBlank
    private String description;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    private LocalDate transactionDate;

    private Long costCenterId;
}
