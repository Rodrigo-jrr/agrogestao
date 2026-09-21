package com.agrogestao.inventory;

import com.agrogestao.inventory.dto.InventoryMovementRequest;
import com.agrogestao.inventory.dto.InventoryMovementResponse;
import com.agrogestao.inventory.dto.ProductRequest;
import com.agrogestao.inventory.dto.ProductResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
    public List<ProductResponse> listProducts() {
        return productService.list();
    }

    @PostMapping("/products")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.create(request));
    }

    @GetMapping("/products/low-stock")
    public List<ProductResponse> lowStock() {
        return productService.listLowStock();
    }

    @GetMapping("/movements")
    public List<InventoryMovementResponse> listMovements() {
        return inventoryMovementService.list();
    }

    @PostMapping("/movements")
    public ResponseEntity<InventoryMovementResponse> createMovement(
            @Valid @RequestBody InventoryMovementRequest request) {
        return ResponseEntity.ok(inventoryMovementService.create(request));
    }
}
