package com.agrogestao.properties;

import com.agrogestao.auth.SecurityUtils;
import com.agrogestao.properties.dto.PropertyDeleteRequest;
import com.agrogestao.properties.dto.PropertyRequest;
import com.agrogestao.properties.dto.PropertySummaryResponse;
import com.agrogestao.properties.dto.PropertyUpdateRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/properties")
public class PropertyController {

    private final PropertyService propertyService;

    public PropertyController(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    @GetMapping("/me")
    public List<PropertySummaryResponse> myProperties() {
        return propertyService.listForUser(SecurityUtils.getCurrentUserId());
    }

    @PostMapping
    public ResponseEntity<PropertySummaryResponse> create(@Valid @RequestBody PropertyRequest request) {
        return ResponseEntity.ok(propertyService.create(SecurityUtils.getCurrentUserId(), request));
    }

    /** Propriedade atual (header X-Property-Id). */
    @PutMapping("/current")
    public PropertySummaryResponse update(@Valid @RequestBody PropertyUpdateRequest request) {
        return propertyService.update(request);
    }

    @DeleteMapping("/current")
    public ResponseEntity<Void> delete(@Valid @RequestBody PropertyDeleteRequest request, HttpServletRequest http) {
        propertyService.delete(request, http.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }
}
