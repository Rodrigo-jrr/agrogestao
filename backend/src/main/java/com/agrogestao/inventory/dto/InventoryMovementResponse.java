package com.agrogestao.inventory.dto;

import com.agrogestao.inventory.MovementType;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InventoryMovementResponse {

    private final Long id;
    private final Long productId;
    private final String productName;
    private final String unit;
    private final MovementType type;
    private final BigDecimal quantity;
    private final BigDecimal unitCost;
    private final LocalDate movementDate;
    private final String notes;
}
