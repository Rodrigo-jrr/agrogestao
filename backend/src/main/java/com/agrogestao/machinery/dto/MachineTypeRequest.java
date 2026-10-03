package com.agrogestao.machinery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MachineTypeRequest(@NotBlank @Size(max = 60) String name) {
}
