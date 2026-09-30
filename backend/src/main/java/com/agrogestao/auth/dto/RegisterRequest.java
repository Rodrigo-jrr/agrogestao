package com.agrogestao.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank
    @Size(max = 150)
    private String name;

    @NotBlank
    @Email
    @Size(max = 180)
    private String email;

    @NotBlank
    @Size(min = 8, max = 72, message = "deve ter entre 8 e 72 caracteres")
    private String password;

    /** Versão dos termos de uso e da política de privacidade que a pessoa leu e aceitou. */
    @NotBlank(message = "é preciso aceitar os termos de uso e a política de privacidade")
    @Size(max = 20)
    private String termsVersion;
}
