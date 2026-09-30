package com.agrogestao.common;

import com.agrogestao.common.exception.BusinessException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

/** Intervalo de datas de uma consulta. Sem datas informadas, usa o mês atual. */
public record Period(LocalDate from, LocalDate to) {

    private static final long MAX_DAYS = 366L * 5;

    public static Period resolve(LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now();
        LocalDate start = from != null ? from : (to != null ? to : today).with(TemporalAdjusters.firstDayOfMonth());
        LocalDate end = to != null ? to : (from != null ? from : today).with(TemporalAdjusters.lastDayOfMonth());
        if (end.isBefore(start)) {
            throw new BusinessException("A data final deve ser igual ou posterior à data inicial.");
        }
        if (ChronoUnit.DAYS.between(start, end) > MAX_DAYS) {
            throw new BusinessException("O período máximo de consulta é de 5 anos.");
        }
        return new Period(start, end);
    }
}
