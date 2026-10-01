package com.agrogestao.livestock.dto;

import java.util.List;

/** Cabeças ativas (animais com brinco + lotes por contagem), no total e por espécie. */
public record HerdSummary(long totalHeads, List<SpeciesCount> bySpecies) {

    public record SpeciesCount(Long speciesId, String speciesName, long heads) {
    }
}
