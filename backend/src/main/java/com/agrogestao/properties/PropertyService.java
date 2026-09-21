package com.agrogestao.properties;

import com.agrogestao.properties.dto.PropertySummaryResponse;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final PropertyMemberRepository propertyMemberRepository;

    public PropertyService(PropertyRepository propertyRepository, PropertyMemberRepository propertyMemberRepository) {
        this.propertyRepository = propertyRepository;
        this.propertyMemberRepository = propertyMemberRepository;
    }

    public List<PropertySummaryResponse> listForUser(Long userId) {
        return propertyMemberRepository.findByUserId(userId).stream()
                .map(member -> {
                    Property property = propertyRepository.findById(member.getPropertyId())
                            .orElseThrow();
                    return new PropertySummaryResponse(
                            property.getId(),
                            property.getOrganizationId(),
                            property.getName(),
                            property.getLocation(),
                            property.getTotalAreaHa(),
                            member.getRole());
                })
                .toList();
    }
}
