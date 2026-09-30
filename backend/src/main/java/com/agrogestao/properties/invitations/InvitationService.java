package com.agrogestao.properties.invitations;

import com.agrogestao.audit.AuditAction;
import com.agrogestao.audit.AuditService;
import com.agrogestao.auth.AttemptLimiter;
import com.agrogestao.auth.AuthService;
import com.agrogestao.auth.EmailService;
import com.agrogestao.auth.SecurityUtils;
import com.agrogestao.auth.User;
import com.agrogestao.auth.UserRepository;
import com.agrogestao.auth.dto.AuthResponse;
import com.agrogestao.auth.token.UserTokenService;
import com.agrogestao.common.exception.BusinessException;
import com.agrogestao.common.exception.ResourceNotFoundException;
import com.agrogestao.common.exception.TooManyRequestsException;
import com.agrogestao.properties.Property;
import com.agrogestao.properties.PropertyMember;
import com.agrogestao.properties.PropertyMemberRepository;
import com.agrogestao.properties.PropertyMemberService;
import com.agrogestao.properties.PropertyRepository;
import com.agrogestao.properties.PropertyRole;
import com.agrogestao.properties.TenantContext;
import com.agrogestao.properties.dto.MemberRequest;
import com.agrogestao.properties.dto.PropertySummaryResponse;
import com.agrogestao.properties.invitations.dto.InvitationPreview;
import com.agrogestao.properties.invitations.dto.InvitationResponse;
import com.agrogestao.properties.invitations.dto.InviteResult;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Entrada na equipe por e-mail. Quem já tem conta entra direto (e recebe um aviso); quem não tem recebe um
 * convite com link de uso único, válido por 7 dias e preso ao e-mail convidado.
 */
@Service
public class InvitationService {

    static final Duration VALIDITY = Duration.ofDays(7);
    private static final String INVALID = "Convite inválido ou expirado. Peça um novo convite a quem enviou.";

    private final PropertyInvitationRepository invitationRepository;
    private final PropertyMemberRepository propertyMemberRepository;
    private final PropertyMemberService propertyMemberService;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final EmailService emailService;
    private final TenantContext tenantContext;
    private final AuditService auditService;
    /** Limita os e-mails de convite por propriedade: nosso servidor de e-mail não pode virar fonte de spam. */
    private final AttemptLimiter sendsByProperty;

    public InvitationService(
            PropertyInvitationRepository invitationRepository,
            PropertyMemberRepository propertyMemberRepository,
            PropertyMemberService propertyMemberService,
            PropertyRepository propertyRepository,
            UserRepository userRepository,
            AuthService authService,
            EmailService emailService,
            TenantContext tenantContext,
            AuditService auditService,
            @Value("${agrogestao.security.invitations.max-per-property-per-day:20}") int maxPerDay) {
        this.invitationRepository = invitationRepository;
        this.propertyMemberRepository = propertyMemberRepository;
        this.propertyMemberService = propertyMemberService;
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
        this.authService = authService;
        this.emailService = emailService;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
        this.sendsByProperty = new AttemptLimiter(maxPerDay, Duration.ofDays(1), Clock.systemUTC());
    }

    @Transactional
    public InviteResult invite(MemberRequest request) {
        tenantContext.requireManageAccess();
        String email = request.getEmail().trim();
        if (userRepository.existsByEmail(email)) {
            return new InviteResult(InviteResult.Outcome.ADDED, propertyMemberService.add(request), null);
        }
        propertyMemberService.requireOwnerToGrant(request.getRole());
        Long propertyId = tenantContext.requirePropertyId();

        // Convidar de novo o mesmo e-mail atualiza o convite (papel e validade) em vez de duplicar.
        PropertyInvitation invitation = invitationRepository
                .findByPropertyIdAndEmail(propertyId, email.toLowerCase(Locale.ROOT))
                .orElseGet(PropertyInvitation::new);
        invitation.setPropertyId(propertyId);
        invitation.setEmail(email.toLowerCase(Locale.ROOT));
        invitation.setRole(request.getRole());
        send(invitation);
        auditService.record("PropertyInvitation", invitation.getId(), AuditAction.CREATE,
                "convite para " + invitation.getEmail() + " como " + invitation.getRole().label());
        return new InviteResult(InviteResult.Outcome.INVITED, null, toResponse(invitation, currentUserName()));
    }

    public List<InvitationResponse> list() {
        tenantContext.requireManageAccess();
        List<PropertyInvitation> invitations =
                invitationRepository.findByPropertyIdOrderByCreatedAtDesc(tenantContext.requirePropertyId());
        Map<Long, String> names = userRepository.findAllById(invitations.stream()
                        .map(PropertyInvitation::getInvitedBy).filter(id -> id != null).toList())
                .stream().collect(Collectors.toMap(User::getId, User::getName));
        return invitations.stream().map(inv -> toResponse(inv, names.get(inv.getInvitedBy()))).toList();
    }

    /** Novo link e mais 7 dias; o link anterior deixa de funcionar. */
    @Transactional
    public InvitationResponse resend(Long id) {
        tenantContext.requireManageAccess();
        PropertyInvitation invitation = findOwned(id);
        propertyMemberService.requireOwnerToGrant(invitation.getRole());
        send(invitation);
        return toResponse(invitation, currentUserName());
    }

    @Transactional
    public void cancel(Long id) {
        tenantContext.requireManageAccess();
        PropertyInvitation invitation = findOwned(id);
        propertyMemberService.requireOwnerToGrant(invitation.getRole());
        invitationRepository.delete(invitation);
        auditService.record("PropertyInvitation", invitation.getId(), AuditAction.DELETE,
                "convite cancelado: " + invitation.getEmail());
    }

    /** Público: a página do convite mostra de onde ele vem antes de a pessoa criar conta ou entrar. */
    public InvitationPreview preview(String token) {
        PropertyInvitation invitation = findValid(token);
        Property property = propertyRepository.findById(invitation.getPropertyId()).orElseThrow();
        String inviter = invitation.getInvitedBy() == null ? null
                : userRepository.findById(invitation.getInvitedBy()).map(User::getName).orElse(null);
        return new InvitationPreview(property.getName(), inviter, invitation.getEmail(), invitation.getRole(),
                userRepository.existsByEmail(invitation.getEmail()));
    }

    /** Aceite por quem já está logado. O convite vale só para a conta com o e-mail convidado. */
    @Transactional
    public PropertySummaryResponse accept(String token) {
        PropertyInvitation invitation = findValid(token);
        User user = userRepository.findById(SecurityUtils.getCurrentUserId()).orElseThrow();
        if (!user.getEmail().equalsIgnoreCase(invitation.getEmail())) {
            throw new BusinessException("Este convite foi enviado para " + invitation.getEmail()
                    + ". Entre com a conta desse e-mail para aceitar.");
        }
        if (!user.isEmailVerified()) {
            // Abrir o link recebido comprova que o e-mail é da pessoa.
            user.setEmailVerifiedAt(LocalDateTime.now());
            userRepository.save(user);
        }
        return join(invitation, user);
    }

    /** Cria a conta com o e-mail do convite e já entra na equipe. */
    @Transactional
    public AuthResponse registerAndAccept(String token, String name, String password, String clientIp) {
        PropertyInvitation invitation = findValid(token);
        User user = authService.registerVerified(name, invitation.getEmail(), password, clientIp);
        join(invitation, user);
        return authService.tokenFor(user);
    }

    private PropertySummaryResponse join(PropertyInvitation invitation, User user) {
        Long propertyId = invitation.getPropertyId();
        PropertyMember member = propertyMemberRepository.findByPropertyIdAndUserId(propertyId, user.getId())
                .orElseGet(() -> {
                    PropertyMember created = new PropertyMember();
                    created.setPropertyId(propertyId);
                    created.setUserId(user.getId());
                    created.setRole(invitation.getRole());
                    propertyMemberRepository.save(created);
                    auditService.record(propertyId, user.getId(), "PropertyMember", created.getId(), AuditAction.CREATE,
                            user.getName() + " (" + user.getEmail() + ") aceitou o convite como "
                                    + created.getRole().label());
                    return created;
                });
        invitationRepository.delete(invitation);
        Property property = propertyRepository.findById(propertyId).orElseThrow();
        return new PropertySummaryResponse(property.getId(), property.getOrganizationId(), property.getName(),
                property.getLocation(), property.getTotalAreaHa(), member.getRole());
    }

    private String send(PropertyInvitation invitation) {
        String key = "invite:" + invitation.getPropertyId();
        if (sendsByProperty.blockedFor(key).isPresent()) {
            throw new TooManyRequestsException("Muitos convites enviados hoje nesta propriedade. Tente amanhã.");
        }
        sendsByProperty.record(key);

        LocalDateTime now = LocalDateTime.now();
        String token = UserTokenService.randomToken();
        invitation.setTokenHash(UserTokenService.hash(token));
        invitation.setInvitedBy(SecurityUtils.getCurrentUserId());
        invitation.setCreatedAt(now);
        invitation.setExpiresAt(now.plus(VALIDITY));
        invitationRepository.save(invitation);

        String propertyName = propertyRepository.findById(invitation.getPropertyId()).map(Property::getName).orElse("");
        emailService.sendInvitation(invitation.getEmail(), currentUserName(), propertyName,
                invitation.getRole().label(), token);
        return token;
    }

    private PropertyInvitation findValid(String token) {
        if (token == null || token.isBlank() || token.length() > 100) {
            throw new BusinessException(INVALID);
        }
        return invitationRepository.findByTokenHash(UserTokenService.hash(token))
                .filter(inv -> !inv.isExpired(LocalDateTime.now()))
                .orElseThrow(() -> new BusinessException(INVALID));
    }

    private PropertyInvitation findOwned(Long id) {
        Long propertyId = tenantContext.requirePropertyId();
        return invitationRepository.findById(id)
                .filter(inv -> inv.getPropertyId().equals(propertyId))
                .orElseThrow(() -> new ResourceNotFoundException("Convite não encontrado"));
    }

    private String currentUserName() {
        return userRepository.findById(SecurityUtils.getCurrentUserId()).map(User::getName).orElse("Alguém");
    }

    private static InvitationResponse toResponse(PropertyInvitation invitation, String invitedByName) {
        return new InvitationResponse(invitation.getId(), invitation.getEmail(), invitation.getRole(), invitedByName,
                invitation.getCreatedAt(), invitation.getExpiresAt(), invitation.isExpired(LocalDateTime.now()));
    }
}
