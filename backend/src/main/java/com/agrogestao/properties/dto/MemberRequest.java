package com.agrogestao.properties.dto;

import com.agrogestao.properties.PropertyRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberRequest {

    @NotBlank
    @Email
    @Size(max = 180)
    private String email;

    @NotNull
    private PropertyRole role;
}
