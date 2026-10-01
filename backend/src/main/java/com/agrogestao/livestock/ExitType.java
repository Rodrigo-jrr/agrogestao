package com.agrogestao.livestock;

/** Saídas sem venda (a venda é feita pela venda de animais, que gera o financeiro). */
public enum ExitType {
    MORTE("Morte", AnimalStatus.MORTO),
    DESCARTE("Descarte", AnimalStatus.DESCARTADO),
    OUTRA("Outra saída", AnimalStatus.DESCARTADO);

    private final String label;
    private final AnimalStatus status;

    ExitType(String label, AnimalStatus status) {
        this.label = label;
        this.status = status;
    }

    public String label() {
        return label;
    }

    public AnimalStatus status() {
        return status;
    }
}
