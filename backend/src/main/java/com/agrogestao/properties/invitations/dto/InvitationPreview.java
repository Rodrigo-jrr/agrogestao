package com.agrogestao.properties.invitations.dto;

import com.agrogestao.properties.PropertyRole;

/** O que a página do convite mostra antes de aceitar. */
public record InvitationPreview(String propertyName, String invitedByName, String email, PropertyRole role,
        boolean accountExists) {
}
