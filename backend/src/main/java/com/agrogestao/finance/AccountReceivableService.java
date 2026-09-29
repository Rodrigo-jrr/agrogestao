package com.agrogestao.finance;

import com.agrogestao.audit.AuditAction;
import com.agrogestao.audit.AuditService;
import com.agrogestao.common.exception.BusinessException;
import com.agrogestao.common.exception.ResourceNotFoundException;
import com.agrogestao.finance.dto.AccountReceivableRequest;
import com.agrogestao.finance.dto.AccountReceivableResponse;
import com.agrogestao.properties.TenantContext;
import com.agrogestao.sales.CustomerRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountReceivableService {

    private final AccountReceivableRepository accountReceivableRepository;
    private final CustomerRepository customerRepository;
    private final FinancialTransactionService financialTransactionService;
    private final FinancialTransactionRepository financialTransactionRepository;
    private final TenantContext tenantContext;
    private final AuditService auditService;

    public AccountReceivableService(
            AccountReceivableRepository accountReceivableRepository,
            CustomerRepository customerRepository,
            FinancialTransactionService financialTransactionService,
            FinancialTransactionRepository financialTransactionRepository,
            TenantContext tenantContext,
            AuditService auditService) {
        this.accountReceivableRepository = accountReceivableRepository;
        this.customerRepository = customerRepository;
        this.financialTransactionService = financialTransactionService;
        this.financialTransactionRepository = financialTransactionRepository;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    public List<AccountReceivableResponse> list() {
        return accountReceivableRepository.findByPropertyIdOrderByDueDateAsc(tenantContext.requirePropertyId())
                .stream().map(this::toResponse).toList();
    }

    public AccountReceivableResponse create(AccountReceivableRequest request) {
        tenantContext.requireWriteAccess();
        Long propertyId = tenantContext.requirePropertyId();
        validateCustomer(propertyId, request.getCustomerId());
        AccountReceivable receivable = new AccountReceivable();
        receivable.setPropertyId(propertyId);
        receivable.setDescription(request.getDescription());
        receivable.setAmount(request.getAmount());
        receivable.setDueDate(request.getDueDate());
        receivable.setCustomerId(request.getCustomerId());
        accountReceivableRepository.save(receivable);
        auditService.record("AccountReceivable", receivable.getId(), AuditAction.CREATE, "%s %s".formatted(receivable.getDescription(), receivable.getAmount()));
        return toResponse(receivable);
    }

    public AccountReceivableResponse update(Long id, AccountReceivableRequest request) {
        tenantContext.requireWriteAccess();
        validateCustomer(tenantContext.requirePropertyId(), request.getCustomerId());
        AccountReceivable receivable = findOwned(id);
        if (receivable.isReceived()) {
            throw new BusinessException("Desfaça a baixa antes de editar esta conta.");
        }
        receivable.setDescription(request.getDescription());
        receivable.setAmount(request.getAmount());
        receivable.setDueDate(request.getDueDate());
        receivable.setCustomerId(request.getCustomerId());
        accountReceivableRepository.save(receivable);
        auditService.record("AccountReceivable", receivable.getId(), AuditAction.UPDATE, "%s %s".formatted(receivable.getDescription(), receivable.getAmount()));
        return toResponse(receivable);
    }

    public void delete(Long id) {
        tenantContext.requireManageAccess();
        AccountReceivable receivable = findOwned(id);
        accountReceivableRepository.delete(receivable);
        auditService.record("AccountReceivable", receivable.getId(), AuditAction.DELETE, "%s %s".formatted(receivable.getDescription(), receivable.getAmount()));
    }

    /** Dá baixa na conta e registra o recebimento como lançamento realizado no caixa. */
    @Transactional
    public AccountReceivableResponse markReceived(Long id) {
        tenantContext.requireWriteAccess();
        AccountReceivable receivable = findOwned(id);
        if (receivable.isReceived()) {
            return toResponse(receivable);
        }
        FinancialTransaction transaction = financialTransactionService.registerFromOperation(
                receivable.getPropertyId(), TransactionType.INCOME, "Recebimento: " + receivable.getDescription(),
                receivable.getAmount(), LocalDate.now(), null);
        receivable.setReceived(true);
        receivable.setFinancialTransactionId(transaction.getId());
        accountReceivableRepository.save(receivable);
        auditService.record("AccountReceivable", receivable.getId(), AuditAction.UPDATE, "baixa: " + "%s %s".formatted(receivable.getDescription(), receivable.getAmount()));
        return toResponse(receivable);
    }

    /** Desfaz a baixa e remove o lançamento gerado por ela. */
    @Transactional
    public AccountReceivableResponse unmarkReceived(Long id) {
        tenantContext.requireWriteAccess();
        AccountReceivable receivable = findOwned(id);
        Long transactionId = receivable.getFinancialTransactionId();
        receivable.setReceived(false);
        receivable.setFinancialTransactionId(null);
        accountReceivableRepository.saveAndFlush(receivable);
        if (transactionId != null) {
            financialTransactionRepository.deleteById(transactionId);
        }
        auditService.record("AccountReceivable", receivable.getId(), AuditAction.UPDATE, "baixa desfeita: " + "%s %s".formatted(receivable.getDescription(), receivable.getAmount()));
        return toResponse(receivable);
    }

    private void validateCustomer(Long propertyId, Long customerId) {
        if (customerId != null && !customerRepository.existsByIdAndPropertyId(customerId, propertyId)) {
            throw new BusinessException("Cliente inválido para esta propriedade");
        }
    }

    private AccountReceivable findOwned(Long id) {
        Long propertyId = tenantContext.requirePropertyId();
        AccountReceivable receivable = accountReceivableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conta a receber não encontrada"));
        if (!receivable.getPropertyId().equals(propertyId)) {
            throw new ResourceNotFoundException("Conta a receber não encontrada");
        }
        return receivable;
    }

    private AccountReceivableResponse toResponse(AccountReceivable receivable) {
        return new AccountReceivableResponse(receivable.getId(), receivable.getDescription(), receivable.getAmount(),
                receivable.getDueDate(), receivable.getCustomerId(), receivable.isReceived());
    }
}
