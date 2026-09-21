package com.agrogestao.finance;

import com.agrogestao.common.exception.ResourceNotFoundException;
import com.agrogestao.finance.dto.AccountPayableRequest;
import com.agrogestao.finance.dto.AccountPayableResponse;
import com.agrogestao.properties.TenantContext;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AccountPayableService {

    private final AccountPayableRepository accountPayableRepository;
    private final TenantContext tenantContext;

    public AccountPayableService(AccountPayableRepository accountPayableRepository, TenantContext tenantContext) {
        this.accountPayableRepository = accountPayableRepository;
        this.tenantContext = tenantContext;
    }

    public List<AccountPayableResponse> list() {
        return accountPayableRepository.findByPropertyIdOrderByDueDateAsc(tenantContext.requirePropertyId())
                .stream().map(this::toResponse).toList();
    }

    public AccountPayableResponse create(AccountPayableRequest request) {
        AccountPayable payable = new AccountPayable();
        payable.setPropertyId(tenantContext.requirePropertyId());
        payable.setDescription(request.getDescription());
        payable.setAmount(request.getAmount());
        payable.setDueDate(request.getDueDate());
        payable.setSupplierId(request.getSupplierId());
        return toResponse(accountPayableRepository.save(payable));
    }

    public AccountPayableResponse markPaid(Long id) {
        AccountPayable payable = findOwned(id);
        payable.setPaid(true);
        return toResponse(accountPayableRepository.save(payable));
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
