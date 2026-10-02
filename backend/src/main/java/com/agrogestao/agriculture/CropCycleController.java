package com.agrogestao.agriculture;

import com.agrogestao.agriculture.dto.CropCycleRequest;
import com.agrogestao.agriculture.dto.CropCycleResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Safras/ciclos de cultivo da propriedade atual (header X-Property-Id). */
@RestController
@RequestMapping("/api/crop-cycles")
public class CropCycleController {

    private final CropCycleService cropCycleService;

    public CropCycleController(CropCycleService cropCycleService) {
        this.cropCycleService = cropCycleService;
    }

    @GetMapping
    public List<CropCycleResponse> list(
            @RequestParam(defaultValue = "false") boolean includeArchived,
            @RequestParam(required = false) Long fieldId) {
        return cropCycleService.list(includeArchived, fieldId);
    }

    @PostMapping
    public CropCycleResponse create(@Valid @RequestBody CropCycleRequest request) {
        return cropCycleService.create(request);
    }

    @PostMapping("/{id}/archive")
    public CropCycleResponse archive(@PathVariable Long id) {
        return cropCycleService.archive(id);
    }

    @PostMapping("/{id}/unarchive")
    public CropCycleResponse unarchive(@PathVariable Long id) {
        return cropCycleService.unarchive(id);
    }
}
