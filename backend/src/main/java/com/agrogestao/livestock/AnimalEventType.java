package com.agrogestao.livestock;

/** Tipos de evento da linha do tempo do rebanho (modelo orientado a eventos, seção 44 do documento-base). */
public enum AnimalEventType {
    ENTRADA,
    MUDANCA_LOTE,
    SAIDA,
    PESAGEM,
    /** Vacina, medicamento ou procedimento; detalhes em {@link HealthApplication}. */
    SANIDADE
}
