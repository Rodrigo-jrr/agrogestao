package com.agrogestao.finance;

import com.agrogestao.audit.AuditAction;
import com.agrogestao.audit.AuditService;
import com.agrogestao.common.Money;
import com.agrogestao.common.PageResponse;
import com.agrogestao.common.exception.BusinessException;
import com.agrogestao.common.exception.ResourceNotFoundException;
import com.agrogestao.finance.dto.AccountPayableRequest;
import com.agrogestao.finance.dto.AccountPayableResponse;
import com.agrogestao.finance.dto.InstallmentInput;
import com.agrogestao.finance.dto.InstallmentView;
import com.agrogestao.properties.TenantContext;
import com.agrogestao.purchases.SupplierRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;
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

    public PageResponse<AccountPayableResponse> list(SettlementStatus status, String query, Pageable pageable) {
        return PageResponse.of(accountPayableRepository.search(tenantContext.requirePropertyId(),
                status.settledFilter(), PageResponse.likePattern(query), pageable), this::toResponse);
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
        auditService.record("AccountPayable", payable.getId(), AuditAction.CREATE, "%s (%s)".formatted(payable.getDescription(), Money.brl(payable.getAmount())));
        return toResponse(payable);
    }

    public AccountPayableResponse update(Long id, AccountPayableRequest request) {
        tenantContext.requireWriteAccess();
        validateSupplier(tenantContext.requirePropertyId(), request.getSupplierId());
        AccountPayable payable = findOwned(id);
        if (payable.isPaid()) {
            throw new BusinessException("Desfaça a baixa antes de editar esta conta.");
        }
        String origin = linkedOperation(payable);
        if (origin != null && (payable.getAmount().compareTo(request.getAmount()) != 0
                || !java.util.Objects.equals(payable.getSupplierId(), request.getSupplierId()))) {
            throw new BusinessException("Esta conta é parcela da " + origin
                    + ": só o vencimento e a descrição podem ser alterados.");
        }
        payable.setDescription(request.getDescription());
        payable.setAmount(request.getAmount());
        payable.setDueDate(request.getDueDate());
        payable.setSupplierId(request.getSupplierId());
        accountPayableRepository.save(payable);
        auditService.record("AccountPayable", payable.getId(), AuditAction.UPDATE, "%s (%s)".formatted(payable.getDescription(), Money.brl(payable.getAmount())));
        return toResponse(payable);
    }

    public void delete(Long id) {
        tenantContext.requireManageAccess();
        AccountPayable payable = findOwned(id);
        String origin = linkedOperation(payable);
        if (origin != null) {
            throw new BusinessException("Esta conta é parcela da " + origin + ". Para removê-la, cancele a "
                    + origin.substring(0, origin.indexOf(" #")) + ".");
        }
        accountPayableRepository.delete(payable);
        auditService.record("AccountPayable", payable.getId(), AuditAction.DELETE, "%s (%s)".formatted(payable.getDescription(), Money.brl(payable.getAmount())));
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
        auditService.record("AccountPayable", payable.getId(), AuditAction.UPDATE, "baixa: " + "%s (%s)".formatted(payable.getDescription(), Money.brl(payable.getAmount())));
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
        auditService.record("AccountPayable", payable.getId(), AuditAction.UPDATE, "baixa desfeita: " + "%s (%s)".formatted(payable.getDescription(), Money.brl(payable.getAmount())));
        return toResponse(payable);
    }

    /** Cria uma conta por parcela da compra a prazo. Chamado dentro da transação da compra. */
    public void createInstallments(Long propertyId, Long supplierId, Long purchaseId,
            List<InstallmentInput> installments) {
        int count = installments.size();
        for (int i = 0; i < count; i++) {
            InstallmentInput installment = installments.get(i);
            AccountPayable payable = new AccountPayable();
            payable.setPropertyId(propertyId);
            payable.setSupplierId(supplierId);
            payable.setPurchaseId(purchaseId);
            payable.setInstallmentNumber(i + 1);
            payable.setInstallmentCount(count);
            payable.setDescription("Compra #" + purchaseId + (count > 1 ? " — parcela " + (i + 1) + "/" + count : ""));
            payable.setAmount(installment.amount());
            payable.setDueDate(installment.dueDate());
            accountPayableRepository.save(payable);
        }
    }

    public List<InstallmentView> installmentsOf(Long purchaseId) {
        return accountPayableRepository.findByPurchaseIdOrderByInstallmentNumberAsc(purchaseId).stream()
                .map(a -> new InstallmentView(a.getId(), a.getInstallmentNumber(), a.getInstallmentCount(),
                        a.getDueDate(), a.getAmount(), a.isPaid()))
                .toList();
    }

    /** Remove as parcelas ao cancelar a compra; exige que nenhuma tenha sido baixada. */
    public void removeInstallments(Long purchaseId) {
        List<AccountPayable> installments = accountPayableRepository.findByPurchaseIdOrderByInstallmentNumberAsc(purchaseId);
        if (installments.stream().anyMatch(AccountPayable::isPaid)) {
            throw new BusinessException("Esta compra tem parcelas já baixadas. Desfaça as baixas em Financeiro antes de cancelar.");
        }
        accountPayableRepository.deleteAll(installments);
    }

    /** Parcelas de uma compra de animais a prazo (pecuária). */
    public void createLivestockInstallments(Long propertyId, Long supplierId, Long tradeId,
            List<InstallmentInput> installments) {
        int count = installments.size();
        for (int i = 0; i < count; i++) {
            InstallmentInput installment = installments.get(i);
            AccountPayable payable = new AccountPayable();
            payable.setPropertyId(propertyId);
            payable.setSupplierId(supplierId);
            payable.setLivestockTradeId(tradeId);
            payable.setInstallmentNumber(i + 1);
            payable.setInstallmentCount(count);
            payable.setDescription("Compra de animais #" + tradeId
                    + (count > 1 ? " — parcela " + (i + 1) + "/" + count : ""));
            payable.setAmount(installment.amount());
            payable.setDueDate(installment.dueDate());
            accountPayableRepository.save(payable);
        }
    }

    public List<InstallmentView> livestockInstallmentsOf(Long tradeId) {
        return accountPayableRepository.findByLivestockTradeIdOrderByInstallmentNumberAsc(tradeId).stream()
                .map(a -> new InstallmentView(a.getId(), a.getInstallmentNumber(), a.getInstallmentCount(),
                        a.getDueDate(), a.getAmount(), a.isPaid()))
                .toList();
    }

    /** Remove as parcelas ao cancelar a compra de animais; exige que nenhuma tenha sido baixada. */
    public void removeLivestockInstallments(Long tradeId) {
        List<AccountPayable> installments = accountPayableRepository.findByLivestockTradeIdOrderByInstallmentNumberAsc(tradeId);
        if (installments.stream().anyMatch(AccountPayable::isPaid)) {
            throw new BusinessException("Esta compra tem parcelas já baixadas. Desfaça as baixas em Financeiro antes de cancelar.");
        }
        accountPayableRepository.deleteAll(installments);
    }

    /** "compra #12" / "compra de animais #3" quando a conta é parcela de uma operação; senão null. */
    private static String linkedOperation(AccountPayable payable) {
        if (payable.getPurchaseId() != null) {
            return "compra #" + payable.getPurchaseId();
        }
        if (payable.getLivestockTradeId() != null) {
            return "compra de animais #" + payable.getLivestockTradeId();
        }
        return null;
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
                payable.getDueDate(), payable.getSupplierId(), payable.isPaid(), payable.getPurchaseId(), payable.getLivestockTradeId(),
                payable.getInstallmentNumber(), payable.getInstallmentCount());
    }
}
