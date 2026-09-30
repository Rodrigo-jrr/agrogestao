package com.agrogestao.finance;

public enum PaymentType {
    /** Pago/recebido na data da operação: vira lançamento no caixa. */
    CASH,
    /** A prazo: cada parcela vira uma conta a pagar/receber. */
    INSTALLMENTS
}
