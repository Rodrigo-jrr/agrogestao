package com.agrogestao.livestock.dto;

import com.agrogestao.livestock.Species;
import java.util.List;

/** Cabeças ativas (animais com brinco + lotes por contagem), no total e por espécie. */
public record HerdSummary(long totalHeads, List<SpeciesCount> bySpecies) {

    public record SpeciesCount(Species species, long heads) {
    }
}
