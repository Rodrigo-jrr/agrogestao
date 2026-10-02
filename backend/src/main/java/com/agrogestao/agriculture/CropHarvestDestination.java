package com.agrogestao.agriculture;

public enum CropHarvestDestination {
    ESTOQUE("Estoque"),
    VENDA("Venda");

    private final String label;

    CropHarvestDestination(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
