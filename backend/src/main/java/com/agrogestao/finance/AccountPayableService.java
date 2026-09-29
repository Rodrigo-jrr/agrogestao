package com.agrogestao.finance;

import com.agrogestao.audit.AuditAction;
import com.agrogestao.audit.AuditService;
import com.agrogestao.common.exception.BusinessException;
import com.agrogestao.common.exception.ResourceNotFoundException;
import com.agrogestao.finance.dto.AccountPayableRequest;
import com.agrogestao.finance.dto.AccountPayableResponse;
import com.agrogestao.properties.TenantContext;
import com.agrogestao.purchases.SupplierRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountPayableService {

    private final AccountPayableRepository accountPayableRepository;
    private final SupplierRepository supplierRepository;
    private final FinancialTransactionService financialTransactionService;
    private final FinancialTransactionRepository financialTransactionRepository;
    private final TenantContext tenantContext;
    private final AuditService auditService;

    public AccountPayableService(
            AccountPayableRepository accountPayableRepository,
            SupplierRepository supplierRepository,
            FinancialTransactionService financialTransactionService,
            FinancialTransactionRepository financialTransactionRepository,
            TenantContext tenantContext,
            AuditService auditService) {
        this.accountPayableRepository = accountPayableRepository;
        this.supplierRepository = supplierRepository;
        this.financialTransactionService = financialTransactionService;
        this.financialTransactionRepository = financialTransactionRepository;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    public List<AccountPayableResponse> list() {
        return accountPayableRepository.findByPropertyIdOrderByDueDateAsc(tenantContext.requirePropertyId())
                .stream().map(this::toResponse).toList();
    }

    public AccountPayableResponse create(AccountPayableRequest request) {
        tenantContext.requireWriteAccess();
        Long propertyId = tenantContext.requirePropertyId();
        validateSupplier(propertyId, request.getSupplierId());
        AccountPayable payable = new AccountPayable();
        payable.setPropertyId(propertyId);
        payable.setDescription(request.getDescription());
        payable.setAmount(request.getAmount());
        payable.setDueDate(request.getDueDate());
        payable.setSupplierId(request.getSupplierId());
        accountPayableRepository.save(payable);
        auditService.record("AccountPayable", payable.getId(), AuditAction.CREATE, "%s %s".formatted(payable.getDescription(), payable.getAmount()));
        return toResponse(payable);
    }

    public AccountPayableResponse update(Long id, AccountPayableRequest request) {
        tenantContext.requireWriteAccess();
        validateSupplier(tenantContext.requirePropertyId(), request.getSupplierId());
        AccountPayable payable = findOwned(id);
        if (payable.isPaid()) {
            throw new BusinessException("Desfaça a baixa antes de editar esta conta.");
        }
        payable.setDescription(request.getDescription());
        payable.setAmount(request.getAmount());
        payable.setDueDate(request.getDueDate());
        payable.setSupplierId(request.getSupplierId());
        accountPayableRepository.save(payable);
        auditService.record("AccountPayable", payable.getId(), AuditAction.UPDATE, "%s %s".formatted(payable.getDescription(), payable.getAmount()));
        return toResponse(payable);
    }

    public void delete(Long id) {
        tenantContext.requireManageAccess();
        AccountPayable payable = findOwned(id);
        accountPayableRepository.delete(payable);
        auditService.record("AccountPayable", payable.getId(), AuditAction.DELETE, "%s %s".formatted(payable.getDescription(), payable.getAmount()));
    }

    /** Dá baixa na conta e registra o pagamento como lançamento realizado no caixa. */
    @Transactional
    public AccountPayableResponse markPaid(Long id) {
        tenantContext.requireWriteAccess();
        AccountPayable payable = findOwned(id);
        if (payable.isPaid()) {
            return toResponse(payable);
        }
        FinancialTransaction transaction = financialTransactionService.registerFromOperation(
                payable.getPropertyId(), TransactionType.EXPENSE, "Pagamento: " + payable.getDescription(),
                payable.getAmount(), LocalDate.now(), null);
        payable.setPaid(true);
        payable.setFinancialTransactionId(transaction.getId());
        accountPayableRepository.save(payable);
        auditService.record("AccountPayable", payable.getId(), AuditAction.UPDATE, "baixa: " + "%s %s".formatted(payable.getDescription(), payable.getAmount()));
        return toResponse(payable);
    }

    /** Desfaz a baixa e remove o lançamento gerado por ela. */
    @Transactional
    public AccountPayableResponse unmarkPaid(Long id) {
        tenantContext.requireWriteAccess();
        AccountPayable payable = findOwned(id);
        Long transactionId = payable.getFinancialTransactionId();
        payable.setPaid(false);
        payable.setFinancialTransactionId(null);
        accountPayableRepository.saveAndFlush(payable);
        if (transactionId != null) {
            financialTransactionRepository.deleteById(transactionId);
        }
        auditService.record("AccountPayable", payable.getId(), AuditAction.UPDATE, "baixa desfeita: " + "%s %s".formatted(payable.getDescription(), payable.getAmount()));
        return toResponse(payable);
    }

    private void validateSupplier(Long propertyId, Long supplierId) {
        if (supplierId != null && !supplierRepository.existsByIdAndPropertyId(supplierId, propertyId)) {
            throw new BusinessException("Fornecedor inválido para esta propriedade");
        }
    }

    private AccountPayable findOwned(Long id) {
        Long propertyId = tenantContext.requirePropertyId();
        AccountPayable payable = accountPayableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conta a pagar não encontrada"));
        if (!payable.getPropertyId().equals(propertyId)) {
            throw new ResourceNotFoundException("Conta a pagar não encontrada");
        }
        return payable;
    }

    private AccountPayableResponse toResponse(AccountPayable payable) {
        return new AccountPayableResponse(payable.getId(), payable.getDescription(), payable.getAmount(),
                payable.getDueDate(), payable.getSupplierId(), payable.isPaid());
    }
}
