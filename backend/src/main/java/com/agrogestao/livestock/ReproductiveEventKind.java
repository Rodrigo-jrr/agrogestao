package com.agrogestao.livestock;

public enum ReproductiveEventKind {
    CIO("Cio"),
    COBERTURA("Cobertura"),
    INSEMINACAO("Inseminação"),
    DIAGNOSTICO_PRENHEZ("Diagnóstico de prenhez"),
    PARTO("Parto"),
    ABORTO("Aborto");

    private final String label;

    ReproductiveEventKind(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
