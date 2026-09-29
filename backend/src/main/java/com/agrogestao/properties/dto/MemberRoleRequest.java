package com.agrogestao.properties.dto;

import com.agrogestao.properties.PropertyRole;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberRoleRequest {

    @NotNull
    private PropertyRole role;
}
