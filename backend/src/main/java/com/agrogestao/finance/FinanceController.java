package com.agrogestao.finance;

import com.agrogestao.common.PageResponse;
import com.agrogestao.common.Period;
import com.agrogestao.finance.dto.AccountPayableRequest;
import com.agrogestao.finance.dto.AccountPayableResponse;
import com.agrogestao.finance.dto.AccountReceivableRequest;
import com.agrogestao.finance.dto.AccountReceivableResponse;
import com.agrogestao.finance.dto.CashFlowResponse;
import com.agrogestao.finance.dto.CostCenterRequest;
import com.agrogestao.finance.dto.CostCenterResponse;
import com.agrogestao.finance.dto.FinancialTransactionRequest;
import com.agrogestao.finance.dto.FinancialTransactionResponse;
import com.agrogestao.finance.dto.TransactionTotals;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/finance")
public class FinanceController {

    private final FinancialTransactionService financialTransactionService;
    private final AccountPayableService accountPayableService;
    private final AccountReceivableService accountReceivableService;
    private final CostCenterService costCenterService;
    private final CashFlowService cashFlowService;

    public FinanceController(
            FinancialTransactionService financialTransactionService,
            AccountPayableService accountPayableService,
            AccountReceivableService accountReceivableService,
            CostCenterService costCenterService,
            CashFlowService cashFlowService) {
        this.financialTransactionService = financialTransactionService;
        this.accountPayableService = accountPayableService;
        this.accountReceivableService = accountReceivableService;
        this.costCenterService = costCenterService;
        this.cashFlowService = cashFlowService;
    }

    @GetMapping("/transactions")
    public PageResponse<FinancialTransactionResponse> listTransactions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) @Size(max = 100) String q,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return financialTransactionService.list(Period.resolve(from, to), type, q, PageResponse.pageable(page, size));
    }

    /** Receitas, despesas e resultado do período inteiro (independe da página). */
    @GetMapping("/transactions/totals")
    public TransactionTotals transactionTotals(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return financialTransactionService.totals(Period.resolve(from, to));
    }

    @PostMapping("/transactions")
    public ResponseEntity<FinancialTransactionResponse> createTransaction(
            @Valid @RequestBody FinancialTransactionRequest request) {
        return ResponseEntity.ok(financialTransactionService.create(request));
    }

    @PutMapping("/transactions/{id}")
    public FinancialTransactionResponse updateTransaction(
            @PathVariable Long id, @Valid @RequestBody FinancialTransactionRequest request) {
        return financialTransactionService.update(id, request);
    }

    @DeleteMapping("/transactions/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id) {
        financialTransactionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** Fluxo de caixa mensal; padrão: 3 meses anteriores até 3 meses à frente. Parâmetros no formato yyyy-MM. */
    @GetMapping("/cash-flow")
    public CashFlowResponse cashFlow(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth to) {
        YearMonth current = YearMonth.now();
        return cashFlowService.monthly(from != null ? from : current.minusMonths(3),
                to != null ? to : current.plusMonths(3));
    }

    @GetMapping("/payables")
    public PageResponse<AccountPayableResponse> listPayables(
            @RequestParam(defaultValue = "ALL") SettlementStatus status,
            @RequestParam(required = false) @Size(max = 100) String q,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return accountPayableService.list(status, q, PageResponse.pageable(page, size));
    }

    @PostMapping("/payables")
    public ResponseEntity<AccountPayableResponse> createPayable(@Valid @RequestBody AccountPayableRequest request) {
        return ResponseEntity.ok(accountPayableService.create(request));
    }

    @PutMapping("/payables/{id}")
    public AccountPayableResponse updatePayable(@PathVariable Long id, @Valid @RequestBody AccountPayableRequest request) {
        return accountPayableService.update(id, request);
    }

    @DeleteMapping("/payables/{id}")
    public ResponseEntity<Void> deletePayable(@PathVariable Long id) {
        accountPayableService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/payables/{id}/pay")
    public AccountPayableResponse payPayable(@PathVariable Long id) {
        return accountPayableService.markPaid(id);
    }

    @PatchMapping("/payables/{id}/unpay")
    public AccountPayableResponse unpayPayable(@PathVariable Long id) {
        return accountPayableService.unmarkPaid(id);
    }

    @GetMapping("/receivables")
    public PageResponse<AccountReceivableResponse> listReceivables(
            @RequestParam(defaultValue = "ALL") SettlementStatus status,
            @RequestParam(required = false) @Size(max = 100) String q,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return accountReceivableService.list(status, q, PageResponse.pageable(page, size));
    }

    @PostMapping("/receivables")
    public ResponseEntity<AccountReceivableResponse> createReceivable(
            @Valid @RequestBody AccountReceivableRequest request) {
        return ResponseEntity.ok(accountReceivableService.create(request));
    }

    @PutMapping("/receivables/{id}")
    public AccountReceivableResponse updateReceivable(
            @PathVariable Long id, @Valid @RequestBody AccountReceivableRequest request) {
        return accountReceivableService.update(id, request);
    }

    @DeleteMapping("/receivables/{id}")
    public ResponseEntity<Void> deleteReceivable(@PathVariable Long id) {
        accountReceivableService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/receivables/{id}/receive")
    public AccountReceivableResponse receiveReceivable(@PathVariable Long id) {
        return accountReceivableService.markReceived(id);
    }

    @PatchMapping("/receivables/{id}/unreceive")
    public AccountReceivableResponse unreceiveReceivable(@PathVariable Long id) {
        return accountReceivableService.unmarkReceived(id);
    }

    @GetMapping("/cost-centers")
    public List<CostCenterResponse> listCostCenters() {
        return costCenterService.list();
    }

    @PostMapping("/cost-centers")
    public ResponseEntity<CostCenterResponse> createCostCenter(@Valid @RequestBody CostCenterRequest request) {
        return ResponseEntity.ok(costCenterService.create(request));
    }

    @PutMapping("/cost-centers/{id}")
    public CostCenterResponse updateCostCenter(@PathVariable Long id, @Valid @RequestBody CostCenterRequest request) {
        return costCenterService.update(id, request);
    }

    @DeleteMapping("/cost-centers/{id}")
    public ResponseEntity<Void> deleteCostCenter(@PathVariable Long id) {
        costCenterService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
