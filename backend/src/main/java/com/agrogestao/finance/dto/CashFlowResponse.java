package com.agrogestao.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CashFlowResponse {

    private final LocalDate from;
    private final LocalDate to;
    private final BigDecimal openingBalance;
    /** Contas em aberto vencidas antes do início do período (entram na projeção do primeiro mês). */
    private final BigDecimal overdueReceivables;
    private final BigDecimal overduePayables;
    private final List<CashFlowMonth> months;
}
