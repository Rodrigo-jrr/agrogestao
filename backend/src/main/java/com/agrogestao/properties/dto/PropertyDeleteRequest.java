package com.agrogestao.properties.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Exclusão definitiva: a pessoa digita o nome da propriedade e confirma a própria senha. */
public record PropertyDeleteRequest(
        @NotBlank @Size(max = 150) String confirmName,
        @NotBlank @Size(max = 72) String password) {
}
