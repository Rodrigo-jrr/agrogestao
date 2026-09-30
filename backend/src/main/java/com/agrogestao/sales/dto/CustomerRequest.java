package com.agrogestao.sales.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerRequest {

    @NotBlank
    @Size(max = 150)
    private String name;

    @Size(max = 30)
    private String document;

    @Size(max = 30)
    private String phone;

    @Email
    @Size(max = 180)
    private String email;
}
