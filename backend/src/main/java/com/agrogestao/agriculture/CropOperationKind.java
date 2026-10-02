package com.agrogestao.agriculture;

public enum CropOperationKind {
    PREPARO("Preparo"),
    PLANTIO("Plantio"),
    ADUBACAO("Adubação"),
    PULVERIZACAO("Pulverização");

    private final String label;

    CropOperationKind(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
