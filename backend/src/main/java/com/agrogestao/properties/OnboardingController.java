package com.agrogestao.properties;

import com.agrogestao.auth.SecurityUtils;
import com.agrogestao.properties.dto.OnboardingRequest;
import com.agrogestao.properties.dto.PropertySummaryResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/onboarding")
public class OnboardingController {

    private final OnboardingService onboardingService;

    public OnboardingController(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @PostMapping
    public ResponseEntity<PropertySummaryResponse> onboard(@Valid @RequestBody OnboardingRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(onboardingService.createPropertyForNewOrganization(userId, request));
    }
}
