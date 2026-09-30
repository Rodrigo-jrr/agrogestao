package com.agrogestao.properties.invitations.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Criação de conta pelo link do convite: o e-mail vem do convite. */
public record InvitationRegisterRequest(
        @NotBlank @Size(max = 100) String token,
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(min = 8, max = 72, message = "deve ter entre 8 e 72 caracteres") String password,
        @NotBlank(message = "é preciso aceitar os termos de uso e a política de privacidade")
        @Size(max = 20) String termsVersion) {
}
