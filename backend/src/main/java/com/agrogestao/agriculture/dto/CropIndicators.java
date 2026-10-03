package com.agrogestao.agriculture.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Indicadores de uma safra, de todas as safras de um talhão, ou de toda a propriedade (conforme o escopo
 * pedido), num período. Sem {@code cropCycleId} nem {@code fieldId}, soma todas as safras da propriedade e
 * {@code totalAreaHa} é a soma das áreas dos talhões distintos envolvidos.
 *
 * <p>{@code harvestedQuantityByUnit}/{@code productionPerHaByUnit}: colheitas da mesma safra podem usar
 * unidades diferentes (parte em sc, parte em kg) — somar direto seria errado, por isso o total e a
 * produção/ha vêm agrupados por unidade.
 *
 * <p>{@code revenue}/{@code revenuePerHa}: só soma colheitas com {@code destination=VENDA}. Colheitas que
 * foram para o estoque não geram receita aqui — ela só existe quando esse produto for vendido de verdade
 * pelo módulo Vendas.
 */
public record CropIndicators(
        Long cropCycleId, String cropCycleLabel,
        Long fieldId, String fieldName,
        LocalDate from, LocalDate to,
        int cycleCount,
        BigDecimal totalAreaHa,
        BigDecimal totalCost, BigDecimal costPerHa,
        Map<String, BigDecimal> harvestedQuantityByUnit, Map<String, BigDecimal> productionPerHaByUnit,
        BigDecimal revenue, BigDecimal revenuePerHa,
        BigDecimal marginPerHa) {
}
