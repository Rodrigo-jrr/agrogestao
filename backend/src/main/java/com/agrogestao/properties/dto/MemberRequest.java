package com.agrogestao.properties.dto;

import com.agrogestao.properties.PropertyRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberRequest {

    @NotBlank
    @Email
    private String email;

    @NotNull
    private PropertyRole role;
}
