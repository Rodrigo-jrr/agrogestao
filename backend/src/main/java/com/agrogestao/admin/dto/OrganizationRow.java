package com.agrogestao.admin.dto;

import java.time.LocalDateTime;
import java.util.List;

public record OrganizationRow(
        Long id,
        String name,
        String ownerName,
        String ownerEmail,
        LocalDateTime createdAt,
        LocalDateTime lastActivityAt,
        List<PropertyRow> properties) {

    public record PropertyRow(
            Long id,
            String name,
            String location,
            java.math.BigDecimal totalAreaHa,
            long members,
            LocalDateTime createdAt,
            LocalDateTime lastActivityAt) {
    }
}
