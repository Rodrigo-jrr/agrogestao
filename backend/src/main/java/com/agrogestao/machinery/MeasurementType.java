package com.agrogestao.machinery;

public enum MeasurementType {
    HOROMETRO("Horímetro"),
    ODOMETRO("Hodômetro");

    private final String label;

    MeasurementType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
