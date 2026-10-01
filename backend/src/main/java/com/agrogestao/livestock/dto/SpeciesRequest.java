package com.agrogestao.livestock.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Cadastro de uma espécie da propriedade. Depois de usada por um animal ou lote, allowsIndividual não muda. */
public record SpeciesRequest(
        @NotBlank @Size(max = 60) String name,
        @NotNull Boolean allowsIndividual) {
}
