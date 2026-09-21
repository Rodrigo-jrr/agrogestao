package com.agrogestao.properties;

import com.agrogestao.auth.SecurityUtils;
import com.agrogestao.properties.dto.PropertySummaryResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
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
}
