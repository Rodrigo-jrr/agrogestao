package com.agrogestao.properties;

public enum PropertyRole {
    OWNER("Proprietário"),
    MANAGER("Gerente"),
    WORKER("Colaborador"),
    VIEWER("Visualizador");

    private final String label;

    PropertyRole(String label) {
        this.label = label;
    }

    /** Nome exibido para as pessoas (e-mails e histórico). */
    public String label() {
        return label;
    }
}
