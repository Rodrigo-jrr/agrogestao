package com.agrogestao.audit.dto;

import com.agrogestao.audit.AuditAction;
import java.time.LocalDateTime;

public record AuditEntryResponse(Long id, LocalDateTime createdAt, Long userId, String userName, String entityName,
        Long entityId, AuditAction action, String details) {
}
