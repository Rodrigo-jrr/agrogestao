package com.agrogestao.livestock;

public enum HealthKind {
    VACINA("Vacina"),
    MEDICAMENTO("Medicamento"),
    /** Consulta, exame, castração, casqueamento... */
    PROCEDIMENTO("Procedimento");

    private final String label;

    HealthKind(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
