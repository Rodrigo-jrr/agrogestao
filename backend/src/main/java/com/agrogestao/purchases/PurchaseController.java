package com.agrogestao.purchases;

import com.agrogestao.purchases.dto.PurchaseRequest;
import com.agrogestao.purchases.dto.PurchaseResponse;
import com.agrogestao.purchases.dto.SupplierRequest;
import com.agrogestao.purchases.dto.SupplierResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {

    private final PurchaseService purchaseService;
    private final SupplierService supplierService;

    public PurchaseController(PurchaseService purchaseService, SupplierService supplierService) {
        this.purchaseService = purchaseService;
        this.supplierService = supplierService;
    }

    @GetMapping
    public List<PurchaseResponse> list() {
        return purchaseService.list();
    }

    @PostMapping
    public ResponseEntity<PurchaseResponse> create(@Valid @RequestBody PurchaseRequest request) {
        return ResponseEntity.ok(purchaseService.create(request));
    }

    @GetMapping("/suppliers")
    public List<SupplierResponse> listSuppliers() {
        return supplierService.list();
    }

    @PostMapping("/suppliers")
    public ResponseEntity<SupplierResponse> createSupplier(@Valid @RequestBody SupplierRequest request) {
        return ResponseEntity.ok(supplierService.create(request));
    }

    @PutMapping("/suppliers/{id}")
    public SupplierResponse updateSupplier(@PathVariable Long id, @Valid @RequestBody SupplierRequest request) {
        return supplierService.update(id, request);
    }

    @DeleteMapping("/suppliers/{id}")
    public ResponseEntity<Void> deleteSupplier(@PathVariable Long id) {
        supplierService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
