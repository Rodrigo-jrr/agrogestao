package com.agrogestao.finance.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountReceivableRequest {

    @NotBlank
    @Size(max = 255)
    private String description;

    @NotNull
    @Positive
    @Digits(integer = 13, fraction = 2, message = "valor acima do permitido ou com mais de 2 casas decimais")
    private BigDecimal amount;

    @NotNull
    private LocalDate dueDate;

    private Long customerId;
}
