package com.agrogestao.production;

import com.agrogestao.production.dto.ProductionRecordRequest;
import com.agrogestao.production.dto.ProductionRecordResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/production")
public class ProductionController {

    private final ProductionRecordService productionRecordService;

    public ProductionController(ProductionRecordService productionRecordService) {
        this.productionRecordService = productionRecordService;
    }

    @GetMapping
    public List<ProductionRecordResponse> list() {
        return productionRecordService.list();
    }

    @PostMapping
    public ResponseEntity<ProductionRecordResponse> create(@Valid @RequestBody ProductionRecordRequest request) {
        return ResponseEntity.ok(productionRecordService.create(request));
    }
}
