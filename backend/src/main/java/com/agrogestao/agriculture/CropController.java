package com.agrogestao.agriculture;

import com.agrogestao.agriculture.dto.CropRequest;
import com.agrogestao.agriculture.dto.CropResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Catálogo de culturas da propriedade atual (header X-Property-Id). */
@RestController
@RequestMapping("/api/crops")
public class CropController {

    private final CropService cropService;

    public CropController(CropService cropService) {
        this.cropService = cropService;
    }

    @GetMapping
    public List<CropResponse> list(@RequestParam(defaultValue = "false") boolean includeArchived) {
        return cropService.list(includeArchived);
    }

    @PostMapping
    public CropResponse create(@Valid @RequestBody CropRequest request) {
        return cropService.create(request);
    }

    @PostMapping("/{id}/archive")
    public CropResponse archive(@PathVariable Long id) {
        return cropService.archive(id);
    }

    @PostMapping("/{id}/unarchive")
    public CropResponse unarchive(@PathVariable Long id) {
        return cropService.unarchive(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        cropService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
