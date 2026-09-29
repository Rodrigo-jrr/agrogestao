package com.agrogestao.properties;

import com.agrogestao.audit.AuditAction;
import com.agrogestao.audit.AuditService;
import com.agrogestao.auth.SecurityUtils;
import com.agrogestao.auth.User;
import com.agrogestao.auth.UserRepository;
import com.agrogestao.common.exception.BusinessException;
import com.agrogestao.common.exception.ForbiddenException;
import com.agrogestao.common.exception.ResourceNotFoundException;
import com.agrogestao.properties.dto.MemberRequest;
import com.agrogestao.properties.dto.MemberResponse;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PropertyMemberService {

    private final PropertyMemberRepository propertyMemberRepository;
    private final UserRepository userRepository;
    private final TenantContext tenantContext;
    private final AuditService auditService;

    public PropertyMemberService(
            PropertyMemberRepository propertyMemberRepository,
            UserRepository userRepository,
            TenantContext tenantContext,
            AuditService auditService) {
        this.propertyMemberRepository = propertyMemberRepository;
        this.userRepository = userRepository;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    public List<MemberResponse> list() {
        List<PropertyMember> members = propertyMemberRepository.findByPropertyId(tenantContext.requirePropertyId());
        Map<Long, User> users = userRepository.findAllById(members.stream().map(PropertyMember::getUserId).toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
        return members.stream()
                .map(member -> toResponse(member, users.get(member.getUserId())))
                .sorted(Comparator.comparing(MemberResponse::getRole).thenComparing(MemberResponse::getName))
                .toList();
    }

    @Transactional
    public MemberResponse add(MemberRequest request) {
        tenantContext.requireManageAccess();
        Long propertyId = tenantContext.requirePropertyId();
        requireOwnerToGrant(request.getRole());

        User user = userRepository.findByEmail(request.getEmail().trim())
                .orElseThrow(() -> new BusinessException(
                        "Nenhum usuário cadastrado com este e-mail. Peça para a pessoa criar uma conta primeiro."));
        if (propertyMemberRepository.existsByPropertyIdAndUserId(propertyId, user.getId())) {
            throw new BusinessException("Este usuário já é membro da propriedade.");
        }

        PropertyMember member = new PropertyMember();
        member.setPropertyId(propertyId);
        member.setUserId(user.getId());
        member.setRole(request.getRole());
        propertyMemberRepository.save(member);
        auditService.record("PropertyMember", member.getId(), AuditAction.CREATE, user.getEmail() + " como " + member.getRole());
        return toResponse(member, user);
    }

    @Transactional
    public MemberResponse updateRole(Long memberId, PropertyRole newRole) {
        tenantContext.requireManageAccess();
        PropertyMember member = findOwned(memberId);
        if (member.getRole() == PropertyRole.OWNER || newRole == PropertyRole.OWNER) {
            requireOwnerToGrant(PropertyRole.OWNER);
        }
        if (member.getRole() == PropertyRole.OWNER && newRole != PropertyRole.OWNER) {
            ensureNotLastOwner(member);
        }
        PropertyRole previousRole = member.getRole();
        member.setRole(newRole);
        propertyMemberRepository.save(member);
        auditService.record("PropertyMember", member.getId(), AuditAction.UPDATE, "usuário #" + member.getUserId() + ": " + previousRole + " -> " + newRole);
        return toResponse(member, userRepository.findById(member.getUserId()).orElse(null));
    }

    @Transactional
    public void remove(Long memberId) {
        tenantContext.requireManageAccess();
        PropertyMember member = findOwned(memberId);
        if (member.getRole() == PropertyRole.OWNER) {
            requireOwnerToGrant(PropertyRole.OWNER);
            ensureNotLastOwner(member);
        }
        propertyMemberRepository.delete(member);
        auditService.record("PropertyMember", member.getId(), AuditAction.DELETE, "usuário #" + member.getUserId() + " (" + member.getRole() + ")");
    }

    /** Somente proprietários podem conceder, alterar ou remover o papel de proprietário. */
    private void requireOwnerToGrant(PropertyRole role) {
        if (role == PropertyRole.OWNER && tenantContext.getRole() != PropertyRole.OWNER) {
            throw new ForbiddenException("Apenas proprietários podem gerenciar outros proprietários.");
        }
    }

    private void ensureNotLastOwner(PropertyMember member) {
        long owners = propertyMemberRepository.countByPropertyIdAndRole(member.getPropertyId(), PropertyRole.OWNER);
        if (owners <= 1) {
            throw new BusinessException("A propriedade precisa ter ao menos um proprietário.");
        }
    }

    private PropertyMember findOwned(Long memberId) {
        Long propertyId = tenantContext.requirePropertyId();
        PropertyMember member = propertyMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Membro não encontrado"));
        if (!member.getPropertyId().equals(propertyId)) {
            throw new ResourceNotFoundException("Membro não encontrado");
        }
        return member;
    }

    private MemberResponse toResponse(PropertyMember member, User user) {
        return new MemberResponse(member.getId(), member.getUserId(),
                user != null ? user.getName() : "(usuário removido)",
                user != null ? user.getEmail() : null, member.getRole(),
                member.getUserId().equals(SecurityUtils.getCurrentUserId()));
    }
}
