package com.agrogestao.admin.dto;

import java.util.List;

public record AdminOverviewResponse(
        Summary summary,
        List<OrganizationRow> organizations,
        List<UserRow> users) {

    public record Summary(
            long organizations,
            long properties,
            long users,
            long newUsersLast30Days,
            long activePropertiesLast30Days) {
    }
}
