package com.agrogestao.inventory.dto;

import com.agrogestao.inventory.MovementType;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InventoryMovementRequest {

    @NotNull
    private Long productId;

    @NotNull
    private MovementType type;

    @NotNull
    private BigDecimal quantity;

    private BigDecimal unitCost;

    @NotNull
    private LocalDate movementDate;

    private String notes;
}
