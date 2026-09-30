package com.agrogestao.purchases.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
    @Size(max = 100, message = "no máximo 100 itens por operação")
    private List<Item> items;

    @Getter
    @Setter
    public static class Item {

        @NotNull
        private Long productId;

        @NotNull
        @Positive
        @Digits(integer = 12, fraction = 3, message = "quantidade acima do permitido ou com mais de 3 casas decimais")
        private BigDecimal quantity;

        @NotNull
        @Positive
        @Digits(integer = 13, fraction = 2, message = "valor acima do permitido ou com mais de 2 casas decimais")
        private BigDecimal unitPrice;
    }
}
