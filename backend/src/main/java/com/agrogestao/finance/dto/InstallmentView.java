package com.agrogestao.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Parcela de uma compra/venda a prazo, como aparece no detalhe da operação. */
public record InstallmentView(Long accountId, int number, int count, LocalDate dueDate, BigDecimal amount, boolean settled) {
}
