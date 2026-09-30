package com.agrogestao.sales.dto;

import com.agrogestao.finance.PaymentType;
import com.agrogestao.finance.dto.InstallmentView;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Detalhe da venda, com custo (médio no momento da venda) e margem. */
public record SaleDetailResponse(
        Long id,
        Long customerId,
        String customerName,
        LocalDate saleDate,
        BigDecimal totalAmount,
        /** Nulo quando algum item não tem custo registrado (vendas anteriores ao custo médio). */
        BigDecimal totalCost,
        BigDecimal margin,
        PaymentType paymentType,
        boolean canceled,
        List<Item> items,
        List<InstallmentView> installments) {

    public record Item(Long productId, String productName, String unit, BigDecimal quantity, BigDecimal unitPrice,
            BigDecimal unitCost, BigDecimal total) {
    }
}
