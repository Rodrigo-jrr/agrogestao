package com.agrogestao.dashboard;

import com.agrogestao.dashboard.dto.DashboardSummaryResponse;
import com.agrogestao.dashboard.dto.ProductionSummaryItem;
import com.agrogestao.finance.FinancialTransactionRepository;
import com.agrogestao.finance.TransactionType;
import com.agrogestao.inventory.ProductService;
import com.agrogestao.production.ProductionRecord;
import com.agrogestao.production.ProductionRecordRepository;
import com.agrogestao.properties.TenantContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final FinancialTransactionRepository financialTransactionRepository;
    private final ProductionRecordRepository productionRecordRepository;
    private final ProductService productService;
    private final TenantContext tenantContext;

    public DashboardService(
            FinancialTransactionRepository financialTransactionRepository,
            ProductionRecordRepository productionRecordRepository,
            ProductService productService,
            TenantContext tenantContext) {
        this.financialTransactionRepository = financialTransactionRepository;
        this.productionRecordRepository = productionRecordRepository;
        this.productService = productService;
        this.tenantContext = tenantContext;
    }

    public DashboardSummaryResponse summary() {
        Long propertyId = tenantContext.requirePropertyId();
        LocalDate from = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth());
        LocalDate to = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

        BigDecimal income = financialTransactionRepository
                .sumByPropertyIdAndTypeAndPeriod(propertyId, TransactionType.INCOME, from, to);
        BigDecimal expense = financialTransactionRepository
                .sumByPropertyIdAndTypeAndPeriod(propertyId, TransactionType.EXPENSE, from, to);

        List<ProductionRecord> records = productionRecordRepository
                .findByPropertyIdAndProductionDateBetween(propertyId, from, to);

        List<ProductionSummaryItem> production = records.stream()
                .collect(Collectors.groupingBy(
                        record -> record.getProductName() + "|" + record.getUnit(),
                        Collectors.reducing(BigDecimal.ZERO, ProductionRecord::getQuantity, BigDecimal::add)))
                .entrySet().stream()
                .map(entry -> {
                    String[] parts = entry.getKey().split("\\|", 2);
                    return new ProductionSummaryItem(parts[0], entry.getValue(), parts[1]);
                })
                .sorted(Comparator.comparing(ProductionSummaryItem::getProductName))
                .toList();

        return new DashboardSummaryResponse(
                from, to, income, expense, income.subtract(expense),
                production, productService.listLowStock());
    }
}
