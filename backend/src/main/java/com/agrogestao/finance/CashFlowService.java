package com.agrogestao.finance;

import com.agrogestao.common.exception.BusinessException;
import com.agrogestao.finance.dto.CashFlowMonth;
import com.agrogestao.finance.dto.CashFlowResponse;
import com.agrogestao.properties.TenantContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class CashFlowService {

    private static final int MAX_MONTHS = 24;
    private static final LocalDate BEGINNING = LocalDate.of(1900, 1, 1);

    private final FinancialTransactionRepository financialTransactionRepository;
    private final AccountPayableRepository accountPayableRepository;
    private final AccountReceivableRepository accountReceivableRepository;
    private final TenantContext tenantContext;

    public CashFlowService(
            FinancialTransactionRepository financialTransactionRepository,
            AccountPayableRepository accountPayableRepository,
            AccountReceivableRepository accountReceivableRepository,
            TenantContext tenantContext) {
        this.financialTransactionRepository = financialTransactionRepository;
        this.accountPayableRepository = accountPayableRepository;
        this.accountReceivableRepository = accountReceivableRepository;
        this.tenantContext = tenantContext;
    }

    public CashFlowResponse monthly(YearMonth fromMonth, YearMonth toMonth) {
        Long propertyId = tenantContext.requirePropertyId();
        if (toMonth.isBefore(fromMonth)) {
            throw new BusinessException("O mês final deve ser igual ou posterior ao mês inicial");
        }
        if (ChronoUnit.MONTHS.between(fromMonth, toMonth) >= MAX_MONTHS) {
            throw new BusinessException("O período máximo do fluxo de caixa é de " + MAX_MONTHS + " meses");
        }
        LocalDate from = fromMonth.atDay(1);
        LocalDate to = toMonth.atEndOfMonth();

        BigDecimal openingBalance = sum(propertyId, TransactionType.INCOME, BEGINNING, from.minusDays(1))
                .subtract(sum(propertyId, TransactionType.EXPENSE, BEGINNING, from.minusDays(1)));

        List<FinancialTransaction> transactions = financialTransactionRepository
                .findByPropertyIdAndTransactionDateBetweenOrderByTransactionDateDesc(propertyId, from, to);
        Map<YearMonth, BigDecimal> income = groupByMonth(transactions.stream()
                .filter(t -> t.getType() == TransactionType.INCOME).toList(),
                t -> YearMonth.from(t.getTransactionDate()), FinancialTransaction::getAmount);
        Map<YearMonth, BigDecimal> expense = groupByMonth(transactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE).toList(),
                t -> YearMonth.from(t.getTransactionDate()), FinancialTransaction::getAmount);

        List<AccountReceivable> openReceivables = accountReceivableRepository
                .findByPropertyIdAndReceivedFalseAndDueDateLessThanEqual(propertyId, to);
        List<AccountPayable> openPayables = accountPayableRepository
                .findByPropertyIdAndPaidFalseAndDueDateLessThanEqual(propertyId, to);

        BigDecimal overdueReceivables = total(openReceivables.stream()
                .filter(r -> r.getDueDate().isBefore(from)).map(AccountReceivable::getAmount).toList());
        BigDecimal overduePayables = total(openPayables.stream()
                .filter(p -> p.getDueDate().isBefore(from)).map(AccountPayable::getAmount).toList());
        Map<YearMonth, BigDecimal> pendingIn = groupByMonth(openReceivables.stream()
                .filter(r -> !r.getDueDate().isBefore(from)).toList(),
                r -> YearMonth.from(r.getDueDate()), AccountReceivable::getAmount);
        Map<YearMonth, BigDecimal> pendingOut = groupByMonth(openPayables.stream()
                .filter(p -> !p.getDueDate().isBefore(from)).toList(),
                p -> YearMonth.from(p.getDueDate()), AccountPayable::getAmount);

        List<CashFlowMonth> months = new ArrayList<>();
        BigDecimal balance = openingBalance;
        BigDecimal pendingAccumulated = overdueReceivables.subtract(overduePayables);
        for (YearMonth month = fromMonth; !month.isAfter(toMonth); month = month.plusMonths(1)) {
            BigDecimal monthIncome = income.getOrDefault(month, BigDecimal.ZERO);
            BigDecimal monthExpense = expense.getOrDefault(month, BigDecimal.ZERO);
            BigDecimal monthPendingIn = pendingIn.getOrDefault(month, BigDecimal.ZERO);
            BigDecimal monthPendingOut = pendingOut.getOrDefault(month, BigDecimal.ZERO);
            BigDecimal net = monthIncome.subtract(monthExpense);
            balance = balance.add(net);
            pendingAccumulated = pendingAccumulated.add(monthPendingIn).subtract(monthPendingOut);
            months.add(new CashFlowMonth(month.toString(), monthIncome, monthExpense, net, balance,
                    monthPendingIn, monthPendingOut, balance.add(pendingAccumulated)));
        }

        return new CashFlowResponse(from, to, openingBalance, overdueReceivables, overduePayables, months);
    }

    private BigDecimal sum(Long propertyId, TransactionType type, LocalDate from, LocalDate to) {
        return financialTransactionRepository.sumByPropertyIdAndTypeAndPeriod(propertyId, type, from, to);
    }

    private static <T> Map<YearMonth, BigDecimal> groupByMonth(
            List<T> items, Function<T, YearMonth> month, Function<T, BigDecimal> amount) {
        return items.stream().collect(Collectors.groupingBy(
                month, Collectors.reducing(BigDecimal.ZERO, amount, BigDecimal::add)));
    }

    private static BigDecimal total(List<BigDecimal> amounts) {
        return amounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
