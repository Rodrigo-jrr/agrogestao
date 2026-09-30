package com.agrogestao.properties.invitations.dto;

import com.agrogestao.properties.PropertyRole;
import java.time.LocalDateTime;

public record InvitationResponse(Long id, String email, PropertyRole role, String invitedByName,
        LocalDateTime createdAt, LocalDateTime expiresAt, boolean expired) {
}
