package com.agrogestao.inventory.dto;

import com.agrogestao.inventory.MovementType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
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
    @PositiveOrZero
    @Digits(integer = 12, fraction = 3, message = "quantidade acima do permitido ou com mais de 3 casas decimais")
    private BigDecimal quantity;

    @PositiveOrZero
    @Digits(integer = 13, fraction = 2, message = "valor acima do permitido ou com mais de 2 casas decimais")
    private BigDecimal unitCost;

    @NotNull
    private LocalDate movementDate;

    @Size(max = 255)
    private String notes;
}
