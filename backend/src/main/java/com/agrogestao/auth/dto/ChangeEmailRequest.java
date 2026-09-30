package com.agrogestao.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeEmailRequest(
        @NotBlank @Email @Size(max = 180) String newEmail,
        @NotBlank @Size(max = 72) String currentPassword) {
}
