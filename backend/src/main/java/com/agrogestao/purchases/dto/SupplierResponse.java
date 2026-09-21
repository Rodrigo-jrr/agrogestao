package com.agrogestao.purchases.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SupplierResponse {

    private final Long id;
    private final String name;
    private final String document;
    private final String phone;
    private final String email;
}
