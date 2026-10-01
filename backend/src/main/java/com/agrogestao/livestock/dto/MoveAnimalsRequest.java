package com.agrogestao.livestock.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/** Troca de lote de vários animais de uma vez. {@code toBatchId} nulo = sem lote. */
public record MoveAnimalsRequest(
        @NotEmpty @Size(max = 500) List<Long> animalIds,
        Long toBatchId,
        @NotNull LocalDate date) {
}
