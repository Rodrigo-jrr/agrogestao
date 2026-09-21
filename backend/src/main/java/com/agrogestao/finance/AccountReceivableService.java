package com.agrogestao.finance;

import com.agrogestao.common.exception.BusinessException;
import com.agrogestao.common.exception.ResourceNotFoundException;
import com.agrogestao.finance.dto.AccountReceivableRequest;
import com.agrogestao.finance.dto.AccountReceivableResponse;
import com.agrogestao.properties.TenantContext;
import com.agrogestao.sales.CustomerRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AccountReceivableService {

    private final AccountReceivableRepository accountReceivableRepository;
    private final CustomerRepository customerRepository;
    private final TenantContext tenantContext;

    public AccountReceivableService(
            AccountReceivableRepository accountReceivableRepository,
            CustomerRepository customerRepository,
            TenantContext tenantContext) {
        this.accountReceivableRepository = accountReceivableRepository;
        this.customerRepository = customerRepository;
        this.tenantContext = tenantContext;
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
        return toResponse(accountReceivableRepository.save(receivable));
    }

    public AccountReceivableResponse update(Long id, AccountReceivableRequest request) {
        tenantContext.requireWriteAccess();
        validateCustomer(tenantContext.requirePropertyId(), request.getCustomerId());
        AccountReceivable receivable = findOwned(id);
        receivable.setDescription(request.getDescription());
        receivable.setAmount(request.getAmount());
        receivable.setDueDate(request.getDueDate());
        receivable.setCustomerId(request.getCustomerId());
        return toResponse(accountReceivableRepository.save(receivable));
    }

    public void delete(Long id) {
        tenantContext.requireManageAccess();
        accountReceivableRepository.delete(findOwned(id));
    }

    public AccountReceivableResponse markReceived(Long id) {
        tenantContext.requireWriteAccess();
        AccountReceivable receivable = findOwned(id);
        receivable.setReceived(true);
        return toResponse(accountReceivableRepository.save(receivable));
    }

    public AccountReceivableResponse unmarkReceived(Long id) {
        tenantContext.requireWriteAccess();
        AccountReceivable receivable = findOwned(id);
        receivable.setReceived(false);
        return toResponse(accountReceivableRepository.save(receivable));
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
