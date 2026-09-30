package com.agrogestao.audit;

import com.agrogestao.audit.dto.AuditAuthor;
import com.agrogestao.audit.dto.AuditEntryResponse;
import com.agrogestao.auth.User;
import com.agrogestao.auth.UserRepository;
import com.agrogestao.common.PageResponse;
import com.agrogestao.common.Period;
import com.agrogestao.common.exception.ForbiddenException;
import com.agrogestao.properties.PropertyRole;
import com.agrogestao.properties.TenantContext;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

/** "Quem fez o quê" na propriedade atual. Só proprietários consultam. */
@Service
public class AuditQueryService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final TenantContext tenantContext;

    public AuditQueryService(AuditLogRepository auditLogRepository, UserRepository userRepository,
            TenantContext tenantContext) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.tenantContext = tenantContext;
    }

    public PageResponse<AuditEntryResponse> search(Period period, Long userId, Set<String> entities, Pageable pageable) {
        Long propertyId = requireOwner();
        Specification<AuditLog> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("propertyId"), propertyId),
                cb.greaterThanOrEqualTo(root.get("createdAt"), period.from().atStartOfDay()),
                cb.lessThan(root.get("createdAt"), period.to().plusDays(1).atStartOfDay()));
        if (userId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("userId"), userId));
        }
        if (entities != null && !entities.isEmpty()) {
            spec = spec.and((root, query, cb) -> root.get("entityName").in(entities));
        }
        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Page<AuditLog> page = auditLogRepository.findAll(spec, sorted);

        Map<Long, String> names = namesOf(page.getContent().stream().map(AuditLog::getUserId).toList());
        return PageResponse.of(page, log -> new AuditEntryResponse(log.getId(), log.getCreatedAt(), log.getUserId(),
                log.getUserId() == null ? null : names.getOrDefault(log.getUserId(), "Usuário removido"),
                log.getEntityName(), log.getEntityId(), log.getAction(), log.getDetails()));
    }

    public List<AuditAuthor> authors() {
        Long propertyId = requireOwner();
        return namesOf(auditLogRepository.findAuthorIds(propertyId)).entrySet().stream()
                .map(entry -> new AuditAuthor(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(AuditAuthor::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private Long requireOwner() {
        Long propertyId = tenantContext.requirePropertyId();
        if (tenantContext.getRole() != PropertyRole.OWNER) {
            throw new ForbiddenException("Apenas proprietários podem ver o histórico de alterações.");
        }
        return propertyId;
    }

    private Map<Long, String> namesOf(List<Long> userIds) {
        return userRepository.findAllById(userIds.stream().filter(id -> id != null).distinct().toList()).stream()
                .collect(Collectors.toMap(User::getId, User::getName));
    }
}
