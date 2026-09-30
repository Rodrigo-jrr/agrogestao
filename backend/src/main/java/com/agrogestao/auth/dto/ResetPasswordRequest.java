package com.agrogestao.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank @Size(max = 100) String token,
        @NotBlank @Size(min = 8, max = 72, message = "deve ter entre 8 e 72 caracteres") String newPassword) {
}
