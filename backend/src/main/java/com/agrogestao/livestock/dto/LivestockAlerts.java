package com.agrogestao.livestock.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Agenda da pecuária: próximos passos sanitários (reforços, fins de tratamento) dos próximos dias e os
 * atrasados, e quem está em carência agora.
 */
public record LivestockAlerts(List<HealthApplicationResponse> followUps, List<Withdrawal> withdrawals) {

    /** Animal (animalId) ou lote por contagem (batchId) em carência. */
    public record Withdrawal(Long animalId, Long batchId, String label, LocalDate until) {
    }
}
