package com.agrogestao.finance;

import com.agrogestao.audit.AuditAction;
import com.agrogestao.audit.AuditService;
import com.agrogestao.auth.SecurityUtils;
import com.agrogestao.common.PageResponse;
import com.agrogestao.common.Period;
import com.agrogestao.common.exception.BusinessException;
import com.agrogestao.common.exception.ResourceNotFoundException;
import com.agrogestao.finance.dto.FinancialTransactionRequest;
import com.agrogestao.finance.dto.FinancialTransactionResponse;
import com.agrogestao.finance.dto.TransactionTotals;
import com.agrogestao.properties.TenantContext;
import com.agrogestao.purchases.PurchaseRepository;
import com.agrogestao.sales.SaleRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class FinancialTransactionService {

    private final FinancialTransactionRepository financialTransactionRepository;
    private final CostCenterRepository costCenterRepository;
    private final AccountPayableRepository accountPayableRepository;
    private final AccountReceivableRepository accountReceivableRepository;
    private final PurchaseRepository purchaseRepository;
    private final SaleRepository saleRepository;
    private final TenantContext tenantContext;
    private final AuditService auditService;

    public FinancialTransactionService(
            FinancialTransactionRepository financialTransactionRepository,
            CostCenterRepository costCenterRepository,
            AccountPayableRepository accountPayableRepository,
            AccountReceivableRepository accountReceivableRepository,
            PurchaseRepository purchaseRepository,
            SaleRepository saleRepository,
            TenantContext tenantContext,
            AuditService auditService) {
        this.financialTransactionRepository = financialTransactionRepository;
        this.costCenterRepository = costCenterRepository;
        this.accountPayableRepository = accountPayableRepository;
        this.accountReceivableRepository = accountReceivableRepository;
        this.purchaseRepository = purchaseRepository;
        this.saleRepository = saleRepository;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    public PageResponse<FinancialTransactionResponse> list(Period period, TransactionType type, String query,
            Pageable pageable) {
        return PageResponse.of(financialTransactionRepository.search(tenantContext.requirePropertyId(),
                period.from(), period.to(), type, PageResponse.likePattern(query), pageable), this::toResponse);
    }

    public TransactionTotals totals(Period period) {
        Long propertyId = tenantContext.requirePropertyId();
        BigDecimal income = financialTransactionRepository
                .sumByPropertyIdAndTypeAndPeriod(propertyId, TransactionType.INCOME, period.from(), period.to());
        BigDecimal expense = financialTransactionRepository
                .sumByPropertyIdAndTypeAndPeriod(propertyId, TransactionType.EXPENSE, period.from(), period.to());
        return new TransactionTotals(income, expense, income.subtract(expense));
    }

    public FinancialTransactionResponse create(FinancialTransactionRequest request) {
        tenantContext.requireWriteAccess();
        Long propertyId = tenantContext.requirePropertyId();
        validateCostCenter(propertyId, request.getCostCenterId());
        FinancialTransaction transaction = new FinancialTransaction();
        transaction.setPropertyId(propertyId);
        transaction.setType(request.getType());
        transaction.setDescription(request.getDescription());
        transaction.setAmount(request.getAmount());
        transaction.setTransactionDate(request.getTransactionDate());
        transaction.setCostCenterId(request.getCostCenterId());
        financialTransactionRepository.save(transaction);

        auditService.record(propertyId, SecurityUtils.getCurrentUserId(), "FinancialTransaction",
                transaction.getId(), AuditAction.CREATE,
                "%s %s".formatted(transaction.getType(), transaction.getAmount()));

        return toResponse(transaction);
    }

    public FinancialTransactionResponse update(Long id, FinancialTransactionRequest request) {
        tenantContext.requireWriteAccess();
        Long propertyId = tenantContext.requirePropertyId();
        validateCostCenter(propertyId, request.getCostCenterId());
        FinancialTransaction transaction = findOwned(id, propertyId);
        ensureNotLinkedToAccount(id);
        transaction.setType(request.getType());
        transaction.setDescription(request.getDescription());
        transaction.setAmount(request.getAmount());
        transaction.setTransactionDate(request.getTransactionDate());
        transaction.setCostCenterId(request.getCostCenterId());
        financialTransactionRepository.save(transaction);

        auditService.record(propertyId, SecurityUtils.getCurrentUserId(), "FinancialTransaction",
                transaction.getId(), AuditAction.UPDATE,
                "%s %s".formatted(transaction.getType(), transaction.getAmount()));

        return toResponse(transaction);
    }

    public void delete(Long id) {
        tenantContext.requireManageAccess();
        Long propertyId = tenantContext.requirePropertyId();
        FinancialTransaction transaction = findOwned(id, propertyId);
        ensureNotLinkedToAccount(id);
        financialTransactionRepository.delete(transaction);

        auditService.record(propertyId, SecurityUtils.getCurrentUserId(), "FinancialTransaction",
                id, AuditAction.DELETE, null);
    }

    public FinancialTransaction registerFromOperation(Long propertyId, TransactionType type, String description,
            BigDecimal amount, LocalDate date, Long costCenterId) {
        FinancialTransaction transaction = new FinancialTransaction();
        transaction.setPropertyId(propertyId);
        transaction.setType(type);
        transaction.setDescription(description);
        transaction.setAmount(amount);
        transaction.setTransactionDate(date);
        transaction.setCostCenterId(costCenterId);
        return financialTransactionRepository.save(transaction);
    }

    private void ensureNotLinkedToAccount(Long transactionId) {
        if (accountPayableRepository.existsByFinancialTransactionId(transactionId)
                || accountReceivableRepository.existsByFinancialTransactionId(transactionId)) {
            throw new BusinessException(
                    "Este lançamento foi gerado pela baixa de uma conta. Desfaça a baixa na aba A pagar/A receber.");
        }
        if (purchaseRepository.existsByFinancialTransactionId(transactionId)
                || saleRepository.existsByFinancialTransactionId(transactionId)) {
            throw new BusinessException(
                    "Este lançamento foi gerado por uma compra ou venda. Para desfazê-lo, cancele a operação de origem.");
        }
    }

    private void validateCostCenter(Long propertyId, Long costCenterId) {
        if (costCenterId != null && !costCenterRepository.existsByIdAndPropertyId(costCenterId, propertyId)) {
            throw new BusinessException("Centro de custo inválido para esta propriedade");
        }
    }

    private FinancialTransaction findOwned(Long id, Long propertyId) {
        FinancialTransaction transaction = financialTransactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lançamento não encontrado"));
        if (!transaction.getPropertyId().equals(propertyId)) {
            throw new ResourceNotFoundException("Lançamento não encontrado");
        }
        return transaction;
    }

    private FinancialTransactionResponse toResponse(FinancialTransaction transaction) {
        return new FinancialTransactionResponse(
                transaction.getId(), transaction.getType(), transaction.getDescription(),
                transaction.getAmount(), transaction.getTransactionDate(), transaction.getCostCenterId());
    }
}
