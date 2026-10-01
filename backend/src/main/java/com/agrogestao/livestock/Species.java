package com.agrogestao.livestock;

/** Espécies atendidas. Aves e peixes são manejados só por lote (contagem), sem cadastro individual. */
public enum Species {
    BOVINO("Bovino", true),
    BUBALINO("Bubalino", true),
    OVINO("Ovino", true),
    CAPRINO("Caprino", true),
    SUINO("Suíno", true),
    EQUINO("Equino", true),
    AVE("Ave", false),
    PEIXE("Peixe", false),
    OUTRO("Outro", true);

    private final String label;
    private final boolean individual;

    Species(String label, boolean individual) {
        this.label = label;
        this.individual = individual;
    }

    public String label() {
        return label;
    }

    /** Permite cadastrar animais um a um (com brinco). */
    public boolean allowsIndividual() {
        return individual;
    }
}
