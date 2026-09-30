package com.agrogestao.purchases;

import com.agrogestao.common.PageResponse;
import com.agrogestao.purchases.dto.PurchaseDetailResponse;
import com.agrogestao.purchases.dto.PurchaseRequest;
import com.agrogestao.purchases.dto.PurchaseResponse;
import com.agrogestao.purchases.dto.SupplierRequest;
import com.agrogestao.purchases.dto.SupplierResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public PageResponse<PurchaseResponse> list(
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return purchaseService.list(supplierId, from, to, PageResponse.pageable(page, size));
    }

    @PostMapping
    public ResponseEntity<PurchaseResponse> create(@Valid @RequestBody PurchaseRequest request) {
        return ResponseEntity.ok(purchaseService.create(request));
    }

    @GetMapping("/{id}")
    public PurchaseDetailResponse detail(@PathVariable Long id) {
        return purchaseService.detail(id);
    }

    @PostMapping("/{id}/cancel")
    public PurchaseResponse cancel(@PathVariable Long id) {
        return purchaseService.cancel(id);
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
