package com.agrogestao.finance;

/** Filtro de contas a pagar/receber: em aberto, baixadas ou todas. */
public enum SettlementStatus {
    OPEN,
    SETTLED,
    ALL;

    /** Valor para o filtro da consulta: null = sem filtro. */
    public Boolean settledFilter() {
        return this == ALL ? null : this == SETTLED;
    }
}
