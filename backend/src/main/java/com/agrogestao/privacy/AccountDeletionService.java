package com.agrogestao.privacy;

import com.agrogestao.audit.AuditAction;
import com.agrogestao.audit.AuditService;
import com.agrogestao.auth.EmailService;
import com.agrogestao.auth.ProfileService;
import com.agrogestao.auth.User;
import com.agrogestao.auth.UserRepository;
import com.agrogestao.common.exception.BusinessException;
import com.agrogestao.properties.Property;
import com.agrogestao.properties.PropertyDataEraser;
import com.agrogestao.properties.PropertyMember;
import com.agrogestao.properties.PropertyMemberRepository;
import com.agrogestao.properties.PropertyRepository;
import com.agrogestao.properties.PropertyRole;
import com.agrogestao.privacy.dto.DeletionPreview;
import com.agrogestao.privacy.dto.DeletionPreview.PropertyRef;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Exclusão da conta pelo titular (LGPD, art. 18, VI). Apaga os dados pessoais e as propriedades em que a
 * pessoa está sozinha; nunca apaga dados de propriedades que outras pessoas usam.
 */
@Service
public class AccountDeletionService {

    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final PropertyMemberRepository propertyMemberRepository;
    private final PropertyDataEraser propertyDataEraser;
    private final ProfileService profileService;
    private final EmailService emailService;
    private final AuditService auditService;
    private final JdbcTemplate jdbcTemplate;

    public AccountDeletionService(
            UserRepository userRepository,
            PropertyRepository propertyRepository,
            PropertyMemberRepository propertyMemberRepository,
            PropertyDataEraser propertyDataEraser,
            ProfileService profileService,
            EmailService emailService,
            AuditService auditService,
            JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.propertyRepository = propertyRepository;
        this.propertyMemberRepository = propertyMemberRepository;
        this.propertyDataEraser = propertyDataEraser;
        this.profileService = profileService;
        this.emailService = emailService;
        this.auditService = auditService;
        this.jdbcTemplate = jdbcTemplate;
    }

    public DeletionPreview preview(Long userId) {
        List<PropertyRef> toDelete = new ArrayList<>();
        List<PropertyRef> toLeave = new ArrayList<>();
        List<PropertyRef> blocking = new ArrayList<>();
        for (PropertyMember membership : propertyMemberRepository.findByUserId(userId)) {
            Property property = propertyRepository.findById(membership.getPropertyId()).orElse(null);
            if (property == null) {
                continue;
            }
            PropertyRef ref = new PropertyRef(property.getId(), property.getName());
            List<PropertyMember> others = propertyMemberRepository.findByPropertyId(property.getId()).stream()
                    .filter(member -> !member.getUserId().equals(userId))
                    .toList();
            if (membership.getRole() != PropertyRole.OWNER) {
                toLeave.add(ref);
            } else if (others.isEmpty()) {
                toDelete.add(ref);
            } else if (others.stream().anyMatch(member -> member.getRole() == PropertyRole.OWNER)) {
                toLeave.add(ref);
            } else {
                blocking.add(ref);
            }
        }
        Comparator<PropertyRef> byName = Comparator.comparing(PropertyRef::name, String.CASE_INSENSITIVE_ORDER);
        toDelete.sort(byName);
        toLeave.sort(byName);
        blocking.sort(byName);
        return new DeletionPreview(toDelete, toLeave, blocking);
    }

    @Transactional
    public void deleteAccount(Long userId, String password, String clientIp) {
        profileService.confirmPassword(userId, password, clientIp);
        DeletionPreview plan = preview(userId);
        if (!plan.canDelete()) {
            throw new BusinessException("Antes de excluir sua conta, torne outra pessoa proprietária ou exclua "
                    + (plan.blocking().size() == 1 ? "a propriedade " : "as propriedades ")
                    + plan.blocking().stream().map(PropertyRef::name).collect(Collectors.joining(", ")) + ".");
        }
        User user = userRepository.findById(userId).orElseThrow();

        for (PropertyRef ref : plan.toDelete()) {
            Long organizationId = propertyRepository.findById(ref.id()).map(Property::getOrganizationId).orElse(null);
            propertyDataEraser.eraseProperty(ref.id());
            if (organizationId != null) {
                propertyDataEraser.eraseOrganizationIfEmpty(organizationId);
            }
        }
        // No histórico das propriedades que continuam, a pessoa vira "usuário removido": tiramos o nome dos
        // registros dos vínculos dela com a equipe e o e-mail de qualquer texto.
        List<Long> membershipIds = jdbcTemplate.queryForList(
                "SELECT id FROM property_members WHERE user_id = ?", Long.class, userId);
        for (Long membershipId : membershipIds) {
            jdbcTemplate.update("UPDATE audit_logs SET details = REPLACE(details, ?, 'usuário removido') "
                    + "WHERE entity_name = 'PropertyMember' AND entity_id = ?", user.getName(), membershipId);
        }
        jdbcTemplate.update("UPDATE audit_logs SET details = REPLACE(details, ?, 'e-mail removido') "
                + "WHERE details LIKE CONCAT('%', ?, '%')", user.getEmail(), user.getEmail());

        jdbcTemplate.update("DELETE FROM property_members WHERE user_id = ?", userId);
        List<Long> organizations = jdbcTemplate.queryForList(
                "SELECT organization_id FROM organization_members WHERE user_id = ?", Long.class, userId);
        jdbcTemplate.update("DELETE FROM organization_members WHERE user_id = ?", userId);
        organizations.forEach(propertyDataEraser::eraseOrganizationIfEmpty);

        jdbcTemplate.update("DELETE FROM user_tokens WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM terms_acceptances WHERE user_id = ?", userId);
        jdbcTemplate.update("UPDATE property_invitations SET invited_by = NULL WHERE invited_by = ?", userId);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);

        // Registro da plataforma sem dado pessoal: só que a conta #id foi excluída pelo titular.
        auditService.record(null, null, "User", userId, AuditAction.DELETE, "conta excluída pelo titular");
        emailService.sendAccountDeletedNotice(user.getEmail(), user.getName());
    }
}
