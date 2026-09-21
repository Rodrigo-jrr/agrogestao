package com.agrogestao.sales;

import com.agrogestao.properties.TenantContext;
import com.agrogestao.sales.dto.CustomerRequest;
import com.agrogestao.sales.dto.CustomerResponse;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final TenantContext tenantContext;

    public CustomerService(CustomerRepository customerRepository, TenantContext tenantContext) {
        this.customerRepository = customerRepository;
        this.tenantContext = tenantContext;
    }

    public List<CustomerResponse> list() {
        return customerRepository.findByPropertyId(tenantContext.requirePropertyId()).stream()
                .map(this::toResponse).toList();
    }

    public CustomerResponse create(CustomerRequest request) {
        Customer customer = new Customer();
        customer.setPropertyId(tenantContext.requirePropertyId());
        customer.setName(request.getName());
        customer.setDocument(request.getDocument());
        customer.setPhone(request.getPhone());
        customer.setEmail(request.getEmail());
        return toResponse(customerRepository.save(customer));
    }

    private CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(customer.getId(), customer.getName(), customer.getDocument(),
                customer.getPhone(), customer.getEmail());
    }
}
