package com.agrogestao.properties;

import com.agrogestao.audit.AuditAction;
import com.agrogestao.audit.AuditService;
import com.agrogestao.auth.ProfileService;
import com.agrogestao.auth.SecurityUtils;
import com.agrogestao.common.exception.BusinessException;
import com.agrogestao.common.exception.ForbiddenException;
import com.agrogestao.organizations.OrganizationMemberRepository;
import com.agrogestao.properties.dto.PropertyDeleteRequest;
import com.agrogestao.properties.dto.PropertyRequest;
import com.agrogestao.properties.dto.PropertySummaryResponse;
import com.agrogestao.properties.dto.PropertyUpdateRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PropertyService {

    private final AuditService auditService;
    private final PropertyRepository propertyRepository;
    private final PropertyMemberRepository propertyMemberRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final TenantContext tenantContext;
    private final ProfileService profileService;
    private final PropertyDataEraser propertyDataEraser;

    public PropertyService(
            PropertyRepository propertyRepository,
            PropertyMemberRepository propertyMemberRepository,
            OrganizationMemberRepository organizationMemberRepository,
            AuditService auditService,
            TenantContext tenantContext,
            ProfileService profileService,
            PropertyDataEraser propertyDataEraser) {
        this.propertyRepository = propertyRepository;
        this.propertyMemberRepository = propertyMemberRepository;
        this.organizationMemberRepository = organizationMemberRepository;
        this.auditService = auditService;
        this.tenantContext = tenantContext;
        this.profileService = profileService;
        this.propertyDataEraser = propertyDataEraser;
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
        auditService.record(property.getId(), userId, "Property", property.getId(), AuditAction.CREATE,
                property.getName());

        return new PropertySummaryResponse(
                property.getId(),
                property.getOrganizationId(),
                property.getName(),
                property.getLocation(),
                property.getTotalAreaHa(),
                PropertyRole.OWNER);
    }

    /** Nome, local e área da propriedade atual. Proprietários e gerentes. */
    @Transactional
    public PropertySummaryResponse update(PropertyUpdateRequest request) {
        tenantContext.requireManageAccess();
        Property property = propertyRepository.findById(tenantContext.requirePropertyId()).orElseThrow();
        String changes = describeChanges(property, request);
        property.setName(request.name().trim());
        property.setLocation(request.location() == null || request.location().isBlank() ? null : request.location().trim());
        property.setTotalAreaHa(request.totalAreaHa());
        propertyRepository.save(property);
        if (!changes.isEmpty()) {
            auditService.record("Property", property.getId(), AuditAction.UPDATE, changes);
        }
        return new PropertySummaryResponse(property.getId(), property.getOrganizationId(), property.getName(),
                property.getLocation(), property.getTotalAreaHa(), tenantContext.getRole());
    }

    /**
     * Exclui a propriedade atual e todos os seus dados, sem volta. Só proprietários, digitando o nome da
     * propriedade e confirmando a própria senha.
     */
    @Transactional
    public void delete(PropertyDeleteRequest request, String clientIp) {
        Long propertyId = tenantContext.requirePropertyId();
        if (tenantContext.getRole() != PropertyRole.OWNER) {
            throw new ForbiddenException("Apenas proprietários podem excluir a propriedade.");
        }
        Property property = propertyRepository.findById(propertyId).orElseThrow();
        if (!property.getName().trim().equals(request.confirmName().trim())) {
            throw new BusinessException("O nome digitado não confere com o nome da propriedade.");
        }
        Long userId = SecurityUtils.getCurrentUserId();
        profileService.confirmPassword(userId, request.password(), clientIp);

        propertyDataEraser.eraseProperty(propertyId);
        propertyDataEraser.eraseOrganizationIfEmpty(property.getOrganizationId());
        // O histórico da propriedade foi junto; fica só este registro, sem vínculo com ela.
        auditService.record(null, userId, "Property", propertyId, AuditAction.DELETE, property.getName());
    }

    private static String describeChanges(Property property, PropertyUpdateRequest request) {
        StringBuilder changes = new StringBuilder();
        if (!property.getName().equals(request.name().trim())) {
            changes.append("nome: ").append(property.getName()).append(" → ").append(request.name().trim()).append("; ");
        }
        String location = request.location() == null || request.location().isBlank() ? null : request.location().trim();
        if (!Objects.equals(property.getLocation(), location)) {
            changes.append("localização: ").append(Objects.requireNonNullElse(location, "(vazia)")).append("; ");
        }
        if (!sameAmount(property.getTotalAreaHa(), request.totalAreaHa())) {
            changes.append("área: ").append(request.totalAreaHa() == null ? "(vazia)" : request.totalAreaHa().stripTrailingZeros().toPlainString().replace('.', ',') + " ha")
                    .append("; ");
        }
        return changes.isEmpty() ? "" : changes.substring(0, changes.length() - 2);
    }

    private static boolean sameAmount(BigDecimal a, BigDecimal b) {
        return a == null ? b == null : b != null && a.compareTo(b) == 0;
    }
}
