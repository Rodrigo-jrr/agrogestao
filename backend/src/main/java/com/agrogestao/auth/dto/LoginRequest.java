package com.agrogestao.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank
    @Size(max = 180)
    private String email;

    @NotBlank
    @Size(max = 200)
    private String password;
}
