package com.agrogestao.sales;

import com.agrogestao.audit.AuditAction;
import com.agrogestao.audit.AuditService;
import com.agrogestao.common.exception.ResourceNotFoundException;
import com.agrogestao.properties.TenantContext;
import com.agrogestao.sales.dto.CustomerRequest;
import com.agrogestao.sales.dto.CustomerResponse;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final TenantContext tenantContext;
    private final AuditService auditService;

    public CustomerService(CustomerRepository customerRepository, TenantContext tenantContext, AuditService auditService) {
        this.customerRepository = customerRepository;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    public List<CustomerResponse> list() {
        return customerRepository.findByPropertyId(tenantContext.requirePropertyId()).stream()
                .map(this::toResponse).toList();
    }

    public CustomerResponse create(CustomerRequest request) {
        tenantContext.requireWriteAccess();
        Customer customer = new Customer();
        customer.setPropertyId(tenantContext.requirePropertyId());
        customer.setName(request.getName());
        customer.setDocument(request.getDocument());
        customer.setPhone(request.getPhone());
        customer.setEmail(request.getEmail());
        customerRepository.save(customer);
        auditService.record("Customer", customer.getId(), AuditAction.CREATE, customer.getName());
        return toResponse(customer);
    }

    public CustomerResponse update(Long id, CustomerRequest request) {
        tenantContext.requireWriteAccess();
        Customer customer = findOwned(id);
        customer.setName(request.getName());
        customer.setDocument(request.getDocument());
        customer.setPhone(request.getPhone());
        customer.setEmail(request.getEmail());
        customerRepository.save(customer);
        auditService.record("Customer", customer.getId(), AuditAction.UPDATE, customer.getName());
        return toResponse(customer);
    }

    public void delete(Long id) {
        tenantContext.requireManageAccess();
        Customer customer = findOwned(id);
        customerRepository.delete(customer);
        auditService.record("Customer", customer.getId(), AuditAction.DELETE, customer.getName());
    }

    private Customer findOwned(Long id) {
        Long propertyId = tenantContext.requirePropertyId();
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));
        if (!customer.getPropertyId().equals(propertyId)) {
            throw new ResourceNotFoundException("Cliente não encontrado");
        }
        return customer;
    }

    private CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(customer.getId(), customer.getName(), customer.getDocument(),
                customer.getPhone(), customer.getEmail());
    }
}
