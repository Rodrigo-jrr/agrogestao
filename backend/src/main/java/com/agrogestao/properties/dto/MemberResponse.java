package com.agrogestao.properties.dto;

import com.agrogestao.properties.PropertyRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MemberResponse {

    private final Long id;
    private final Long userId;
    private final String name;
    private final String email;
    private final PropertyRole role;
    private final boolean currentUser;
}
