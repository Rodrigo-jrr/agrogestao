package com.agrogestao.common;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Valores em reais para textos lidos por pessoas (histórico de alterações, e-mails). */
public final class Money {

    private static final Locale PT_BR = Locale.of("pt", "BR");

    private Money() {
    }

    /** 1234.5 -> "R$ 1.234,50" */
    public static String brl(BigDecimal amount) {
        if (amount == null) {
            return "-";
        }
        return "R$ " + new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(PT_BR)).format(amount);
    }
}
