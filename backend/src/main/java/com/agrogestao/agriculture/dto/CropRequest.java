package com.agrogestao.agriculture.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CropRequest(@NotBlank @Size(max = 60) String name) {
}
