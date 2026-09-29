package com.agrogestao.purchases;

import com.agrogestao.audit.AuditAction;
import com.agrogestao.audit.AuditService;
import com.agrogestao.common.exception.ResourceNotFoundException;
import com.agrogestao.properties.TenantContext;
import com.agrogestao.purchases.dto.SupplierRequest;
import com.agrogestao.purchases.dto.SupplierResponse;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final TenantContext tenantContext;
    private final AuditService auditService;

    public SupplierService(SupplierRepository supplierRepository, TenantContext tenantContext, AuditService auditService) {
        this.supplierRepository = supplierRepository;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    public List<SupplierResponse> list() {
        return supplierRepository.findByPropertyId(tenantContext.requirePropertyId()).stream()
                .map(this::toResponse).toList();
    }

    public SupplierResponse create(SupplierRequest request) {
        tenantContext.requireWriteAccess();
        Supplier supplier = new Supplier();
        supplier.setPropertyId(tenantContext.requirePropertyId());
        supplier.setName(request.getName());
        supplier.setDocument(request.getDocument());
        supplier.setPhone(request.getPhone());
        supplier.setEmail(request.getEmail());
        supplierRepository.save(supplier);
        auditService.record("Supplier", supplier.getId(), AuditAction.CREATE, supplier.getName());
        return toResponse(supplier);
    }

    public SupplierResponse update(Long id, SupplierRequest request) {
        tenantContext.requireWriteAccess();
        Supplier supplier = findOwned(id);
        supplier.setName(request.getName());
        supplier.setDocument(request.getDocument());
        supplier.setPhone(request.getPhone());
        supplier.setEmail(request.getEmail());
        supplierRepository.save(supplier);
        auditService.record("Supplier", supplier.getId(), AuditAction.UPDATE, supplier.getName());
        return toResponse(supplier);
    }

    public void delete(Long id) {
        tenantContext.requireManageAccess();
        Supplier supplier = findOwned(id);
        supplierRepository.delete(supplier);
        auditService.record("Supplier", supplier.getId(), AuditAction.DELETE, supplier.getName());
    }

    private Supplier findOwned(Long id) {
        Long propertyId = tenantContext.requirePropertyId();
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor não encontrado"));
        if (!supplier.getPropertyId().equals(propertyId)) {
            throw new ResourceNotFoundException("Fornecedor não encontrado");
        }
        return supplier;
    }

    private SupplierResponse toResponse(Supplier supplier) {
        return new SupplierResponse(supplier.getId(), supplier.getName(), supplier.getDocument(),
                supplier.getPhone(), supplier.getEmail());
    }
}
