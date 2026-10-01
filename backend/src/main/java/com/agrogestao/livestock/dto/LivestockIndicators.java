package com.agrogestao.livestock.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Indicadores da pecuária num lote (ou de toda a propriedade, sem {@code batchId}) num período.
 * Taxas em percentual (0–100); ficam nulas quando não há base de cálculo no período (ex.: sem diagnóstico
 * de prenhez para a taxa de prenhez, sem fêmeas ativas para a taxa de natalidade).
 *
 * <p>Custo: {@code purchaseCost} (já rateado por animal na compra) + {@code healthCost} (já rateado por
 * animal/lote na sanidade) + {@code generalCost} (despesas do centro de custo da atividade do lote no
 * período — ração, mão de obra etc. — rateadas igualmente pelas cabeças). {@code costPerAnimal} e
 * {@code costPerKg} dividem o total pelas cabeças e pelo peso vivo total do escopo.
 */
public record LivestockIndicators(
        Long batchId, String batchName, LocalDate from, LocalDate to, long headCount,
        int pregnancyDiagnoses, int positivePregnancyDiagnoses, BigDecimal pregnancyRate,
        int births, BigDecimal birthRate,
        int deaths, BigDecimal mortalityRate,
        BigDecimal averageCalvingIntervalDays,
        BigDecimal averageDailyGain,
        BigDecimal purchaseCost, BigDecimal healthCost, BigDecimal generalCost, BigDecimal totalCost,
        BigDecimal costPerAnimal, BigDecimal costPerKg) {
}
