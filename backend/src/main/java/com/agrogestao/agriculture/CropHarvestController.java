package com.agrogestao.agriculture;

import com.agrogestao.agriculture.dto.CropHarvestRequest;
import com.agrogestao.agriculture.dto.CropHarvestResponse;
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

/** Colheitas da propriedade atual (header X-Property-Id). */
@RestController
@RequestMapping("/api/crop-harvests")
public class CropHarvestController {

    private final CropHarvestService cropHarvestService;

    public CropHarvestController(CropHarvestService cropHarvestService) {
        this.cropHarvestService = cropHarvestService;
    }

    @GetMapping
    public PageResponse<CropHarvestResponse> list(
            @RequestParam(required = false) Long cropCycleId,
            @RequestParam(required = false) Long fieldId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return cropHarvestService.list(cropCycleId, fieldId, from, to, PageResponse.pageable(page, size));
    }

    @PostMapping
    public CropHarvestResponse create(@Valid @RequestBody CropHarvestRequest request) {
        return cropHarvestService.create(request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        cropHarvestService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
