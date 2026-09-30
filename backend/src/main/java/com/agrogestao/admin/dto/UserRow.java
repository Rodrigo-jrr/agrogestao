package com.agrogestao.admin.dto;

import java.time.LocalDateTime;

public record UserRow(
        Long id,
        String name,
        String email,
        long properties,
        boolean platformAdmin,
        LocalDateTime createdAt,
        LocalDateTime lastActivityAt) {
}
