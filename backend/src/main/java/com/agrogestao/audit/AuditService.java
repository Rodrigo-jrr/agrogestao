package com.agrogestao.audit;

import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void record(Long propertyId, Long userId, String entityName, Long entityId,
            AuditAction action, String details) {
        AuditLog log = new AuditLog();
        log.setPropertyId(propertyId);
        log.setUserId(userId);
        log.setEntityName(entityName);
        log.setEntityId(entityId);
        log.setAction(action);
        log.setDetails(details);
        log.setCreatedAt(LocalDateTime.now());
        auditLogRepository.save(log);
    }
}
