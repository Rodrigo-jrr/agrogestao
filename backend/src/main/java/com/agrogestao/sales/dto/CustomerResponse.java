package com.agrogestao.sales.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CustomerResponse {

    private final Long id;
    private final String name;
    private final String document;
    private final String phone;
    private final String email;
}
