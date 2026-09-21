package com.agrogestao.finance;

import com.agrogestao.audit.AuditAction;
import com.agrogestao.audit.AuditService;
import com.agrogestao.auth.SecurityUtils;
import com.agrogestao.finance.dto.FinancialTransactionRequest;
import com.agrogestao.finance.dto.FinancialTransactionResponse;
import com.agrogestao.properties.TenantContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FinancialTransactionService {

    private final FinancialTransactionRepository financialTransactionRepository;
    private final TenantContext tenantContext;
    private final AuditService auditService;

    public FinancialTransactionService(
            FinancialTransactionRepository financialTransactionRepository,
            TenantContext tenantContext,
            AuditService auditService) {
        this.financialTransactionRepository = financialTransactionRepository;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    public List<FinancialTransactionResponse> list(LocalDate from, LocalDate to) {
        Long propertyId = tenantContext.requirePropertyId();
        return financialTransactionRepository
                .findByPropertyIdAndTransactionDateBetweenOrderByTransactionDateDesc(propertyId, from, to)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public FinancialTransactionResponse create(FinancialTransactionRequest request) {
        Long propertyId = tenantContext.requirePropertyId();
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

    private FinancialTransactionResponse toResponse(FinancialTransaction transaction) {
        return new FinancialTransactionResponse(
                transaction.getId(), transaction.getType(), transaction.getDescription(),
                transaction.getAmount(), transaction.getTransactionDate(), transaction.getCostCenterId());
    }
}
