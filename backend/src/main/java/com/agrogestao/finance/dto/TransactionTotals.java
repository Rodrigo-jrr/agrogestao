package com.agrogestao.finance.dto;

import java.math.BigDecimal;

public record TransactionTotals(BigDecimal income, BigDecimal expense, BigDecimal result) {
}
