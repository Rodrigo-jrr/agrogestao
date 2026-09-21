package com.agrogestao.properties;

import com.agrogestao.common.exception.ForbiddenException;
import com.agrogestao.organizations.OrganizationMemberRepository;
import com.agrogestao.properties.dto.PropertyRequest;
import com.agrogestao.properties.dto.PropertySummaryResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final PropertyMemberRepository propertyMemberRepository;
    private final OrganizationMemberRepository organizationMemberRepository;

    public PropertyService(
            PropertyRepository propertyRepository,
            PropertyMemberRepository propertyMemberRepository,
            OrganizationMemberRepository organizationMemberRepository) {
        this.propertyRepository = propertyRepository;
        this.propertyMemberRepository = propertyMemberRepository;
        this.organizationMemberRepository = organizationMemberRepository;
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

    @Transactional
    public PropertySummaryResponse create(Long userId, PropertyRequest request) {
        boolean belongsToOrganization = organizationMemberRepository
                .existsByOrganizationIdAndUserId(request.getOrganizationId(), userId);
        if (!belongsToOrganization) {
            throw new ForbiddenException("Você não pertence a esta organização");
        }

        Property property = new Property();
        property.setOrganizationId(request.getOrganizationId());
        property.setName(request.getName());
        property.setLocation(request.getLocation());
        property.setTotalAreaHa(request.getTotalAreaHa());
        propertyRepository.save(property);

        PropertyMember propertyMember = new PropertyMember();
        propertyMember.setPropertyId(property.getId());
        propertyMember.setUserId(userId);
        propertyMember.setRole(PropertyRole.OWNER);
        propertyMemberRepository.save(propertyMember);

        return new PropertySummaryResponse(
                property.getId(),
                property.getOrganizationId(),
                property.getName(),
                property.getLocation(),
                property.getTotalAreaHa(),
                PropertyRole.OWNER);
    }
}
