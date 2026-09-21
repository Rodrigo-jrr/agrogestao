package com.agrogestao.finance;

import com.agrogestao.common.exception.ResourceNotFoundException;
import com.agrogestao.finance.dto.AccountReceivableRequest;
import com.agrogestao.finance.dto.AccountReceivableResponse;
import com.agrogestao.properties.TenantContext;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AccountReceivableService {

    private final AccountReceivableRepository accountReceivableRepository;
    private final TenantContext tenantContext;

    public AccountReceivableService(
            AccountReceivableRepository accountReceivableRepository, TenantContext tenantContext) {
        this.accountReceivableRepository = accountReceivableRepository;
        this.tenantContext = tenantContext;
    }

    public List<AccountReceivableResponse> list() {
        return accountReceivableRepository.findByPropertyIdOrderByDueDateAsc(tenantContext.requirePropertyId())
                .stream().map(this::toResponse).toList();
    }

    public AccountReceivableResponse create(AccountReceivableRequest request) {
        AccountReceivable receivable = new AccountReceivable();
        receivable.setPropertyId(tenantContext.requirePropertyId());
        receivable.setDescription(request.getDescription());
        receivable.setAmount(request.getAmount());
        receivable.setDueDate(request.getDueDate());
        receivable.setCustomerId(request.getCustomerId());
        return toResponse(accountReceivableRepository.save(receivable));
    }

    public AccountReceivableResponse markReceived(Long id) {
        AccountReceivable receivable = findOwned(id);
        receivable.setReceived(true);
        return toResponse(accountReceivableRepository.save(receivable));
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
