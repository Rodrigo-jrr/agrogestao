package com.agrogestao.finance.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CashFlowMonth {

    /** Mês no formato yyyy-MM. */
    private final String month;
    private final BigDecimal income;
    private final BigDecimal expense;
    private final BigDecimal net;
    /** Saldo realizado acumulado ao final do mês. */
    private final BigDecimal balance;
    private final BigDecimal pendingReceivables;
    private final BigDecimal pendingPayables;
    /** Saldo realizado + contas em aberto acumuladas até o fim do mês. */
    private final BigDecimal projectedBalance;
}
