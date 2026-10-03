package com.agrogestao.machinery.dto;

import com.agrogestao.machinery.MeasurementType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Cadastro de uma máquina. {@code currentReading} só vale na criação (leitura inicial); para atualizar a
 * leitura depois, use o endpoint dedicado — editar aqui não altera a leitura guardada.
 */
public record MachineRequest(
        @NotBlank @Size(max = 150) String name,
        @NotNull Long machineTypeId,
        @Size(max = 60) String identification,
        @NotNull MeasurementType measurementType,
        @DecimalMin(value = "0", message = "não pode ser negativa")
        @Digits(integer = 10, fraction = 2, message = "acima do permitido ou com mais de 2 casas decimais")
        BigDecimal currentReading) {
}
