package com.agrogestao.machinery.dto;

import com.agrogestao.machinery.MeasurementType;
import java.math.BigDecimal;

public record MachineResponse(
        Long id,
        String name,
        Long machineTypeId,
        String machineTypeName,
        String identification,
        MeasurementType measurementType,
        BigDecimal currentReading,
        boolean archived) {
}
