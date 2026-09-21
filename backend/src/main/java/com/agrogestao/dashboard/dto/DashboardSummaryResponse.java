package com.agrogestao.dashboard.dto;

import com.agrogestao.inventory.dto.ProductResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DashboardSummaryResponse {

    private final LocalDate periodFrom;
    private final LocalDate periodTo;
    private final BigDecimal totalIncome;
    private final BigDecimal totalExpense;
    private final BigDecimal result;
    private final List<ProductionSummaryItem> production;
    private final List<ProductResponse> lowStockAlerts;
}
