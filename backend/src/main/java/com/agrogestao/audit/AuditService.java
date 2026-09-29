package com.agrogestao.audit;

import com.agrogestao.auth.SecurityUtils;
import com.agrogestao.properties.TenantContext;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private static final int MAX_DETAILS_LENGTH = 500;

    private final AuditLogRepository auditLogRepository;
    private final TenantContext tenantContext;

    public AuditService(AuditLogRepository auditLogRepository, TenantContext tenantContext) {
        this.auditLogRepository = auditLogRepository;
        this.tenantContext = tenantContext;
    }

    /** Registra a ação na propriedade atual (X-Property-Id), atribuída ao usuário autenticado. */
    public void record(String entityName, Long entityId, AuditAction action, String details) {
        record(tenantContext.requirePropertyId(), SecurityUtils.getCurrentUserId(), entityName, entityId,
                action, details);
    }

    public void record(Long propertyId, Long userId, String entityName, Long entityId,
            AuditAction action, String details) {
        AuditLog log = new AuditLog();
        log.setPropertyId(propertyId);
        log.setUserId(userId);
        log.setEntityName(entityName);
        log.setEntityId(entityId);
        log.setAction(action);
        log.setDetails(details != null && details.length() > MAX_DETAILS_LENGTH
                ? details.substring(0, MAX_DETAILS_LENGTH) : details);
        log.setCreatedAt(LocalDateTime.now());
        auditLogRepository.save(log);
    }
}
