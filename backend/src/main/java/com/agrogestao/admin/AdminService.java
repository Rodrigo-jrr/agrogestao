package com.agrogestao.admin;

import com.agrogestao.admin.dto.AdminOverviewResponse;
import com.agrogestao.admin.dto.OrganizationRow;
import com.agrogestao.admin.dto.OrganizationRow.PropertyRow;
import com.agrogestao.admin.dto.UserRow;
import com.agrogestao.audit.AuditAction;
import com.agrogestao.audit.AuditLogRepository;
import com.agrogestao.audit.AuditService;
import com.agrogestao.auth.SecurityUtils;
import com.agrogestao.auth.User;
import com.agrogestao.auth.UserRepository;
import com.agrogestao.organizations.Organization;
import com.agrogestao.organizations.OrganizationMember;
import com.agrogestao.organizations.OrganizationMemberRepository;
import com.agrogestao.organizations.OrganizationRepository;
import com.agrogestao.organizations.OrganizationRole;
import com.agrogestao.properties.Property;
import com.agrogestao.properties.PropertyMemberRepository;
import com.agrogestao.properties.PropertyRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Visão consolidada (somente leitura) de todas as organizações, propriedades e usuários. */
@Service
public class AdminService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final PropertyRepository propertyRepository;
    private final PropertyMemberRepository propertyMemberRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditService auditService;
    private final PlatformAdminService platformAdminService;

    public AdminService(
            OrganizationRepository organizationRepository,
            OrganizationMemberRepository organizationMemberRepository,
            PropertyRepository propertyRepository,
            PropertyMemberRepository propertyMemberRepository,
            UserRepository userRepository,
            AuditLogRepository auditLogRepository,
            AuditService auditService,
            PlatformAdminService platformAdminService) {
        this.organizationRepository = organizationRepository;
        this.organizationMemberRepository = organizationMemberRepository;
        this.propertyRepository = propertyRepository;
        this.propertyMemberRepository = propertyMemberRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.auditService = auditService;
        this.platformAdminService = platformAdminService;
    }

    @Transactional
    public AdminOverviewResponse overview() {
        Long adminId = SecurityUtils.getCurrentUserId();
        auditService.record(null, adminId, "AdminConsole", adminId, AuditAction.VIEW, "visão geral da plataforma");

        Map<Long, User> users = userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, Long> membersByProperty = toLongMap(propertyMemberRepository.countMembersByProperty());
        Map<Long, Long> propertiesByUser = toLongMap(propertyMemberRepository.countPropertiesByUser());
        Map<Long, LocalDateTime> activityByProperty = toDateMap(auditLogRepository.lastActivityByProperty());
        Map<Long, LocalDateTime> activityByUser = toDateMap(auditLogRepository.lastActivityByUser());

        Map<Long, List<Property>> propertiesByOrg = propertyRepository.findAll().stream()
                .collect(Collectors.groupingBy(Property::getOrganizationId));
        Map<Long, User> ownerByOrg = organizationMemberRepository.findAll().stream()
                .filter(member -> member.getRole() == OrganizationRole.OWNER)
                .filter(member -> users.containsKey(member.getUserId())) // ignora vínculos órfãos
                .sorted(Comparator.comparing(OrganizationMember::getId))
                .collect(Collectors.toMap(OrganizationMember::getOrganizationId,
                        member -> users.get(member.getUserId()), (first, ignored) -> first));

        List<OrganizationRow> organizations = organizationRepository.findAll().stream()
                .sorted(Comparator.comparing(Organization::getCreatedAt).reversed())
                .map(org -> {
                    List<PropertyRow> properties = propertiesByOrg.getOrDefault(org.getId(), List.of()).stream()
                            .sorted(Comparator.comparing(Property::getName, String.CASE_INSENSITIVE_ORDER))
                            .map(p -> new PropertyRow(p.getId(), p.getName(), p.getLocation(), p.getTotalAreaHa(),
                                    membersByProperty.getOrDefault(p.getId(), 0L), p.getCreatedAt(),
                                    activityByProperty.get(p.getId())))
                            .toList();
                    User owner = ownerByOrg.get(org.getId());
                    LocalDateTime lastActivity = properties.stream().map(PropertyRow::lastActivityAt)
                            .filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null);
                    return new OrganizationRow(org.getId(), org.getName(),
                            owner != null ? owner.getName() : null, owner != null ? owner.getEmail() : null,
                            org.getCreatedAt(), lastActivity, properties);
                })
                .toList();

        List<UserRow> userRows = users.values().stream()
                .sorted(Comparator.comparing(User::getCreatedAt).reversed())
                .map(user -> new UserRow(user.getId(), user.getName(), user.getEmail(),
                        propertiesByUser.getOrDefault(user.getId(), 0L),
                        platformAdminService.isAdmin(user.getId()),
                        user.getCreatedAt(), activityByUser.get(user.getId())))
                .toList();

        LocalDateTime monthAgo = LocalDateTime.now().minusDays(30);
        long propertyCount = propertiesByOrg.values().stream().mapToLong(List::size).sum();
        var summary = new AdminOverviewResponse.Summary(
                organizations.size(),
                propertyCount,
                userRows.size(),
                userRows.stream().filter(u -> u.createdAt().isAfter(monthAgo)).count(),
                organizations.stream().flatMap(org -> org.properties().stream())
                        .filter(p -> p.lastActivityAt() != null && p.lastActivityAt().isAfter(monthAgo)).count());

        return new AdminOverviewResponse(summary, organizations, userRows);
    }

    private static Map<Long, Long> toLongMap(List<Object[]> rows) {
        return rows.stream().collect(Collectors.toMap(row -> (Long) row[0], row -> ((Number) row[1]).longValue()));
    }

    private static Map<Long, LocalDateTime> toDateMap(List<Object[]> rows) {
        return rows.stream().collect(Collectors.toMap(row -> (Long) row[0], row -> (LocalDateTime) row[1]));
    }
}
