package com.agrogestao.livestock.dto;

import com.agrogestao.livestock.AnimalOrigin;
import com.agrogestao.livestock.Sex;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Cadastro (e edição) de um animal. Na edição, origem, data de entrada, lote e peso são ignorados:
 * eles mudam por eventos (troca de lote, pesagem...).
 */
public record AnimalRequest(
        @NotBlank @Size(max = 30) String tag,
        @Size(max = 100) String name,
        @NotNull Long speciesId,
        @Size(max = 60) String breed,
        @NotNull Sex sex,
        LocalDate birthDate,
        Boolean birthDateEstimated,
        AnimalOrigin origin,
        LocalDate entryDate,
        Long motherId,
        @Size(max = 100) String sire,
        Long batchId,
        @DecimalMin(value = "0.01", message = "deve ser maior que zero")
        @Digits(integer = 6, fraction = 2, message = "acima do permitido ou com mais de 2 casas decimais")
        BigDecimal weight,
        @Size(max = 500) String notes) {
}
