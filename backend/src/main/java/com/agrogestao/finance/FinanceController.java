package com.agrogestao.finance;

import com.agrogestao.finance.dto.AccountPayableRequest;
import com.agrogestao.finance.dto.AccountPayableResponse;
import com.agrogestao.finance.dto.AccountReceivableRequest;
import com.agrogestao.finance.dto.AccountReceivableResponse;
import com.agrogestao.finance.dto.CostCenterRequest;
import com.agrogestao.finance.dto.CostCenterResponse;
import com.agrogestao.finance.dto.FinancialTransactionRequest;
import com.agrogestao.finance.dto.FinancialTransactionResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    public FinanceController(
            FinancialTransactionService financialTransactionService,
            AccountPayableService accountPayableService,
            AccountReceivableService accountReceivableService,
            CostCenterService costCenterService) {
        this.financialTransactionService = financialTransactionService;
        this.accountPayableService = accountPayableService;
        this.accountReceivableService = accountReceivableService;
        this.costCenterService = costCenterService;
    }

    @GetMapping("/transactions")
    public List<FinancialTransactionResponse> listTransactions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate effectiveFrom = from != null ? from : LocalDate.now().with(TemporalAdjusters.firstDayOfMonth());
        LocalDate effectiveTo = to != null ? to : LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());
        return financialTransactionService.list(effectiveFrom, effectiveTo);
    }

    @PostMapping("/transactions")
    public ResponseEntity<FinancialTransactionResponse> createTransaction(
            @Valid @RequestBody FinancialTransactionRequest request) {
        return ResponseEntity.ok(financialTransactionService.create(request));
    }

    @GetMapping("/payables")
    public List<AccountPayableResponse> listPayables() {
        return accountPayableService.list();
    }

    @PostMapping("/payables")
    public ResponseEntity<AccountPayableResponse> createPayable(@Valid @RequestBody AccountPayableRequest request) {
        return ResponseEntity.ok(accountPayableService.create(request));
    }

    @PatchMapping("/payables/{id}/pay")
    public AccountPayableResponse payPayable(@PathVariable Long id) {
        return accountPayableService.markPaid(id);
    }

    @GetMapping("/receivables")
    public List<AccountReceivableResponse> listReceivables() {
        return accountReceivableService.list();
    }

    @PostMapping("/receivables")
    public ResponseEntity<AccountReceivableResponse> createReceivable(
            @Valid @RequestBody AccountReceivableRequest request) {
        return ResponseEntity.ok(accountReceivableService.create(request));
    }

    @PatchMapping("/receivables/{id}/receive")
    public AccountReceivableResponse receiveReceivable(@PathVariable Long id) {
        return accountReceivableService.markReceived(id);
    }

    @GetMapping("/cost-centers")
    public List<CostCenterResponse> listCostCenters() {
        return costCenterService.list();
    }

    @PostMapping("/cost-centers")
    public ResponseEntity<CostCenterResponse> createCostCenter(@Valid @RequestBody CostCenterRequest request) {
        return ResponseEntity.ok(costCenterService.create(request));
    }
}
