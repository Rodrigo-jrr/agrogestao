package com.agrogestao.livestock.dto;

import java.util.List;

/** Ficha do animal: dados, linha do tempo (mais recente primeiro) e crias. */
public record AnimalDetailResponse(AnimalResponse animal, List<AnimalEventResponse> events,
        List<AnimalResponse> offspring) {
}
