package com.agrogestao.livestock.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Agenda da pecuária: próximos passos sanitários (reforços, fins de tratamento) dos próximos dias e os
 * atrasados, quem está em carência agora, partos previstos para breve e fêmeas cobertas sem diagnóstico.
 */
public record LivestockAlerts(List<HealthApplicationResponse> followUps, List<Withdrawal> withdrawals,
        List<UpcomingCalving> upcomingCalvings, List<PendingDiagnosis> pendingDiagnoses) {

    /** Animal (animalId) ou lote por contagem (batchId) em carência. */
    public record Withdrawal(Long animalId, Long batchId, String label, LocalDate until) {
    }

    /** Gestação em aberto com previsão de parto nos próximos dias (ou já atrasada). */
    public record UpcomingCalving(Long animalId, String animalTag, LocalDate expectedCalvingDate) {
    }

    /** Fêmea coberta ou inseminada há tempo suficiente para diagnosticar a prenhez, sem diagnóstico ainda. */
    public record PendingDiagnosis(Long animalId, String animalTag, LocalDate since) {
    }
}
