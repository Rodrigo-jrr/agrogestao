package com.agrogestao.livestock.dto;

import com.agrogestao.finance.PaymentType;
import com.agrogestao.livestock.TradeKind;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TradeResponse(Long id, TradeKind kind, Long partyId, String partyName, LocalDate tradeDate,
        int headCount, BigDecimal totalAmount, PaymentType paymentType, int installmentCount, Long batchId,
        String batchName, String notes, boolean canceled) {
}
