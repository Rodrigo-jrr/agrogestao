package com.agrogestao.agriculture;

import com.agrogestao.agriculture.dto.CropOperationRequest;
import com.agrogestao.agriculture.dto.CropOperationResponse;
import com.agrogestao.common.PageResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Operações agrícolas da propriedade atual (header X-Property-Id). */
@RestController
@RequestMapping("/api/crop-operations")
public class CropOperationController {

    private final CropOperationService cropOperationService;

    public CropOperationController(CropOperationService cropOperationService) {
        this.cropOperationService = cropOperationService;
    }

    @GetMapping
    public PageResponse<CropOperationResponse> list(
            @RequestParam(required = false) Long cropCycleId,
            @RequestParam(required = false) Long fieldId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return cropOperationService.list(cropCycleId, fieldId, from, to, PageResponse.pageable(page, size));
    }

    @PostMapping
    public CropOperationResponse create(@Valid @RequestBody CropOperationRequest request) {
        return cropOperationService.create(request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        cropOperationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
