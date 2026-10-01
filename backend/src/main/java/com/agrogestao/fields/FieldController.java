package com.agrogestao.fields;

import com.agrogestao.fields.dto.FieldRequest;
import com.agrogestao.fields.dto.FieldResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Talhões da propriedade atual (header X-Property-Id). */
@RestController
@RequestMapping("/api/fields")
public class FieldController {

    private final FieldService fieldService;

    public FieldController(FieldService fieldService) {
        this.fieldService = fieldService;
    }

    @GetMapping
    public List<FieldResponse> list(@RequestParam(defaultValue = "false") boolean includeArchived) {
        return fieldService.list(includeArchived);
    }

    @PostMapping
    public FieldResponse create(@Valid @RequestBody FieldRequest request) {
        return fieldService.create(request);
    }

    @PutMapping("/{id}")
    public FieldResponse update(@PathVariable Long id, @Valid @RequestBody FieldRequest request) {
        return fieldService.update(id, request);
    }

    @PostMapping("/{id}/archive")
    public FieldResponse archive(@PathVariable Long id) {
        return fieldService.archive(id);
    }

    @PostMapping("/{id}/unarchive")
    public FieldResponse unarchive(@PathVariable Long id) {
        return fieldService.unarchive(id);
    }
}
