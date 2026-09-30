package com.agrogestao.inventory;

import com.agrogestao.common.Period;
import com.agrogestao.inventory.dto.InventoryMovementRequest;
import com.agrogestao.inventory.dto.InventoryMovementResponse;
import com.agrogestao.inventory.dto.ProductRequest;
import com.agrogestao.inventory.dto.ProductResponse;
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
@RequestMapping("/api/inventory")
public class InventoryController {

    private final ProductService productService;
    private final InventoryMovementService inventoryMovementService;

    public InventoryController(ProductService productService, InventoryMovementService inventoryMovementService) {
        this.productService = productService;
        this.inventoryMovementService = inventoryMovementService;
    }

    @GetMapping("/products")
    public List<ProductResponse> listProducts(@RequestParam(defaultValue = "false") boolean includeArchived) {
        return productService.list(includeArchived);
    }

    @PostMapping("/products")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.create(request));
    }

    @PutMapping("/products/{id}")
    public ProductResponse updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return productService.update(id, request);
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/products/{id}/archive")
    public ProductResponse archiveProduct(@PathVariable Long id) {
        return productService.archive(id);
    }

    @PostMapping("/products/{id}/unarchive")
    public ProductResponse unarchiveProduct(@PathVariable Long id) {
        return productService.unarchive(id);
    }

    @GetMapping("/products/low-stock")
    public List<ProductResponse> lowStock() {
        return productService.listLowStock();
    }

    @GetMapping("/movements")
    public List<InventoryMovementResponse> listMovements(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return inventoryMovementService.list(productId, Period.resolve(from, to));
    }

    @PostMapping("/movements")
    public ResponseEntity<InventoryMovementResponse> createMovement(
            @Valid @RequestBody InventoryMovementRequest request) {
        return ResponseEntity.ok(inventoryMovementService.create(request));
    }
}
