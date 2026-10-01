package com.agrogestao.finance;

import com.agrogestao.audit.AuditAction;
import com.agrogestao.audit.AuditService;
import com.agrogestao.common.Money;
import com.agrogestao.common.PageResponse;
import com.agrogestao.common.exception.BusinessException;
import com.agrogestao.common.exception.ResourceNotFoundException;
import com.agrogestao.finance.dto.AccountReceivableRequest;
import com.agrogestao.finance.dto.AccountReceivableResponse;
import com.agrogestao.finance.dto.InstallmentInput;
import com.agrogestao.finance.dto.InstallmentView;
import com.agrogestao.properties.TenantContext;
import com.agrogestao.sales.CustomerRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;
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

    public PageResponse<AccountReceivableResponse> list(SettlementStatus status, String query, Pageable pageable) {
        return PageResponse.of(accountReceivableRepository.search(tenantContext.requirePropertyId(),
                status.settledFilter(), PageResponse.likePattern(query), pageable), this::toResponse);
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
        auditService.record("AccountReceivable", receivable.getId(), AuditAction.CREATE, "%s (%s)".formatted(receivable.getDescription(), Money.brl(receivable.getAmount())));
        return toResponse(receivable);
    }

    public AccountReceivableResponse update(Long id, AccountReceivableRequest request) {
        tenantContext.requireWriteAccess();
        validateCustomer(tenantContext.requirePropertyId(), request.getCustomerId());
        AccountReceivable receivable = findOwned(id);
        if (receivable.isReceived()) {
            throw new BusinessException("Desfaça a baixa antes de editar esta conta.");
        }
        String origin = linkedOperation(receivable);
        if (origin != null && (receivable.getAmount().compareTo(request.getAmount()) != 0
                || !java.util.Objects.equals(receivable.getCustomerId(), request.getCustomerId()))) {
            throw new BusinessException("Esta conta é parcela da " + origin
                    + ": só o vencimento e a descrição podem ser alterados.");
        }
        receivable.setDescription(request.getDescription());
        receivable.setAmount(request.getAmount());
        receivable.setDueDate(request.getDueDate());
        receivable.setCustomerId(request.getCustomerId());
        accountReceivableRepository.save(receivable);
        auditService.record("AccountReceivable", receivable.getId(), AuditAction.UPDATE, "%s (%s)".formatted(receivable.getDescription(), Money.brl(receivable.getAmount())));
        return toResponse(receivable);
    }

    public void delete(Long id) {
        tenantContext.requireManageAccess();
        AccountReceivable receivable = findOwned(id);
        String origin = linkedOperation(receivable);
        if (origin != null) {
            throw new BusinessException("Esta conta é parcela da " + origin + ". Para removê-la, cancele a "
                    + origin.substring(0, origin.indexOf(" #")) + ".");
        }
        accountReceivableRepository.delete(receivable);
        auditService.record("AccountReceivable", receivable.getId(), AuditAction.DELETE, "%s (%s)".formatted(receivable.getDescription(), Money.brl(receivable.getAmount())));
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
        auditService.record("AccountReceivable", receivable.getId(), AuditAction.UPDATE, "baixa: " + "%s (%s)".formatted(receivable.getDescription(), Money.brl(receivable.getAmount())));
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
        auditService.record("AccountReceivable", receivable.getId(), AuditAction.UPDATE, "baixa desfeita: " + "%s (%s)".formatted(receivable.getDescription(), Money.brl(receivable.getAmount())));
        return toResponse(receivable);
    }

    /** Cria uma conta por parcela da venda a prazo. Chamado dentro da transação da venda. */
    public void createInstallments(Long propertyId, Long customerId, Long saleId,
            List<InstallmentInput> installments) {
        int count = installments.size();
        for (int i = 0; i < count; i++) {
            InstallmentInput installment = installments.get(i);
            AccountReceivable receivable = new AccountReceivable();
            receivable.setPropertyId(propertyId);
            receivable.setCustomerId(customerId);
            receivable.setSaleId(saleId);
            receivable.setInstallmentNumber(i + 1);
            receivable.setInstallmentCount(count);
            receivable.setDescription("Venda #" + saleId + (count > 1 ? " — parcela " + (i + 1) + "/" + count : ""));
            receivable.setAmount(installment.amount());
            receivable.setDueDate(installment.dueDate());
            accountReceivableRepository.save(receivable);
        }
    }

    public List<InstallmentView> installmentsOf(Long saleId) {
        return accountReceivableRepository.findBySaleIdOrderByInstallmentNumberAsc(saleId).stream()
                .map(a -> new InstallmentView(a.getId(), a.getInstallmentNumber(), a.getInstallmentCount(),
                        a.getDueDate(), a.getAmount(), a.isReceived()))
                .toList();
    }

    /** Remove as parcelas ao cancelar a venda; exige que nenhuma tenha sido baixada. */
    public void removeInstallments(Long saleId) {
        List<AccountReceivable> installments = accountReceivableRepository.findBySaleIdOrderByInstallmentNumberAsc(saleId);
        if (installments.stream().anyMatch(AccountReceivable::isReceived)) {
            throw new BusinessException("Esta venda tem parcelas já baixadas. Desfaça as baixas em Financeiro antes de cancelar.");
        }
        accountReceivableRepository.deleteAll(installments);
    }

    /** Parcelas de uma venda de animais a prazo (pecuária). */
    public void createLivestockInstallments(Long propertyId, Long customerId, Long tradeId,
            List<InstallmentInput> installments) {
        int count = installments.size();
        for (int i = 0; i < count; i++) {
            InstallmentInput installment = installments.get(i);
            AccountReceivable receivable = new AccountReceivable();
            receivable.setPropertyId(propertyId);
            receivable.setCustomerId(customerId);
            receivable.setLivestockTradeId(tradeId);
            receivable.setInstallmentNumber(i + 1);
            receivable.setInstallmentCount(count);
            receivable.setDescription("Venda de animais #" + tradeId
                    + (count > 1 ? " — parcela " + (i + 1) + "/" + count : ""));
            receivable.setAmount(installment.amount());
            receivable.setDueDate(installment.dueDate());
            accountReceivableRepository.save(receivable);
        }
    }

    public List<InstallmentView> livestockInstallmentsOf(Long tradeId) {
        return accountReceivableRepository.findByLivestockTradeIdOrderByInstallmentNumberAsc(tradeId).stream()
                .map(a -> new InstallmentView(a.getId(), a.getInstallmentNumber(), a.getInstallmentCount(),
                        a.getDueDate(), a.getAmount(), a.isReceived()))
                .toList();
    }

    /** Remove as parcelas ao cancelar a venda de animais; exige que nenhuma tenha sido baixada. */
    public void removeLivestockInstallments(Long tradeId) {
        List<AccountReceivable> installments = accountReceivableRepository.findByLivestockTradeIdOrderByInstallmentNumberAsc(tradeId);
        if (installments.stream().anyMatch(AccountReceivable::isReceived)) {
            throw new BusinessException("Esta venda tem parcelas já baixadas. Desfaça as baixas em Financeiro antes de cancelar.");
        }
        accountReceivableRepository.deleteAll(installments);
    }

    /** "venda #12" / "venda de animais #3" quando a conta é parcela de uma operação; senão null. */
    private static String linkedOperation(AccountReceivable receivable) {
        if (receivable.getSaleId() != null) {
            return "venda #" + receivable.getSaleId();
        }
        if (receivable.getLivestockTradeId() != null) {
            return "venda de animais #" + receivable.getLivestockTradeId();
        }
        return null;
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
                receivable.getDueDate(), receivable.getCustomerId(), receivable.isReceived(), receivable.getSaleId(), receivable.getLivestockTradeId(),
                receivable.getInstallmentNumber(), receivable.getInstallmentCount());
    }
}
