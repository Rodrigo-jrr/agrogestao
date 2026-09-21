package com.agrogestao.properties;

import com.agrogestao.activities.Activity;
import com.agrogestao.activities.ActivityRepository;
import com.agrogestao.organizations.Organization;
import com.agrogestao.organizations.OrganizationMember;
import com.agrogestao.organizations.OrganizationMemberRepository;
import com.agrogestao.organizations.OrganizationRepository;
import com.agrogestao.organizations.OrganizationRole;
import com.agrogestao.properties.dto.OnboardingRequest;
import com.agrogestao.properties.dto.PropertySummaryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OnboardingService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final PropertyRepository propertyRepository;
    private final PropertyMemberRepository propertyMemberRepository;
    private final ActivityRepository activityRepository;

    public OnboardingService(
            OrganizationRepository organizationRepository,
            OrganizationMemberRepository organizationMemberRepository,
            PropertyRepository propertyRepository,
            PropertyMemberRepository propertyMemberRepository,
            ActivityRepository activityRepository) {
        this.organizationRepository = organizationRepository;
        this.organizationMemberRepository = organizationMemberRepository;
        this.propertyRepository = propertyRepository;
        this.propertyMemberRepository = propertyMemberRepository;
        this.activityRepository = activityRepository;
    }

    @Transactional
    public PropertySummaryResponse createPropertyForNewOrganization(Long userId, OnboardingRequest request) {
        Organization organization = new Organization();
        organization.setName(request.getOrganizationName());
        organizationRepository.save(organization);

        OrganizationMember orgMember = new OrganizationMember();
        orgMember.setOrganizationId(organization.getId());
        orgMember.setUserId(userId);
        orgMember.setRole(OrganizationRole.OWNER);
        organizationMemberRepository.save(orgMember);

        Property property = new Property();
        property.setOrganizationId(organization.getId());
        property.setName(request.getPropertyName());
        property.setLocation(request.getLocation());
        property.setTotalAreaHa(request.getTotalAreaHa());
        propertyRepository.save(property);

        PropertyMember propertyMember = new PropertyMember();
        propertyMember.setPropertyId(property.getId());
        propertyMember.setUserId(userId);
        propertyMember.setRole(PropertyRole.OWNER);
        propertyMemberRepository.save(propertyMember);

        request.getActivities().forEach(activityInput -> {
            Activity activity = new Activity();
            activity.setPropertyId(property.getId());
            activity.setName(activityInput.getName());
            activity.setType(activityInput.getType());
            activity.setAreaHa(activityInput.getAreaHa());
            activityRepository.save(activity);
        });

        return new PropertySummaryResponse(
                property.getId(),
                organization.getId(),
                property.getName(),
                property.getLocation(),
                property.getTotalAreaHa(),
                PropertyRole.OWNER);
    }
}
