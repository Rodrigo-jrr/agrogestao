package com.agrogestao.sales.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerRequest {

    @NotBlank
    private String name;

    private String document;

    private String phone;

    private String email;
}
