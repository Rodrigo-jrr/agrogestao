package com.agrogestao.livestock.dto;

import com.agrogestao.livestock.ExitType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Entrada ou saída de cabeças num lote por contagem, sem compra/venda (nascimento, eclosão, morte...).
 * {@code exitType} só vale para saídas.
 */
public record BatchCountRequest(
        @NotNull @Positive @Max(1_000_000) Integer headCount,
        @NotNull LocalDate date,
        ExitType exitType,
        @Size(max = 255) String description) {
}
