package com.agrogestao.livestock.dto;

import com.agrogestao.livestock.ExitType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record AnimalExitRequest(
        @NotNull ExitType type,
        @NotNull LocalDate date,
        @Size(max = 255) String reason) {
}
