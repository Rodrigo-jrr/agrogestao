package com.agrogestao.purchases.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PurchaseRequest {

    @NotNull
    private Long supplierId;

    @NotNull
    private LocalDate purchaseDate;

    @NotEmpty
    @Valid
    private List<Item> items;

    @Getter
    @Setter
    public static class Item {

        @NotNull
        private Long productId;

        @NotNull
        @Positive
        private BigDecimal quantity;

        @NotNull
        @Positive
        private BigDecimal unitPrice;
    }
}
