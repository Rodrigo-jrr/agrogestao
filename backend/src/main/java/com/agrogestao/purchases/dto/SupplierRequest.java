package com.agrogestao.purchases.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SupplierRequest {

    @NotBlank
    private String name;

    private String document;

    private String phone;

    private String email;
}
