package com.agrogestao.livestock.dto;

public record SpeciesResponse(Long id, String name, boolean allowsIndividual, boolean archived) {
}
