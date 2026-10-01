package com.agrogestao.livestock.dto;

import com.agrogestao.livestock.PregnancyResult;
import com.agrogestao.livestock.ReproductiveEventKind;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * Evento reprodutivo de uma fêmea. Campos usados conforme {@code kind}: {@code sire} em cobertura/inseminação;
 * {@code pregnancyResult} (e {@code expectedCalvingDate}, se positivo) no diagnóstico de prenhez; {@code offspring}
 * no parto (uma entrada por cria, que entra no rebanho como nascimento). O diagnóstico em aberto que um parto
 * ou aborto encerra é resolvido pelo servidor — não é informado aqui.
 */
public record ReproductiveEventRequest(
        @NotNull Long animalId,
        @NotNull ReproductiveEventKind kind,
        @NotNull LocalDate date,
        @Size(max = 100) String sire,
        PregnancyResult pregnancyResult,
        LocalDate expectedCalvingDate,
        /** Lote das crias no parto; se omitido, usa o lote atual da mãe. */
        Long batchId,
        @Valid @Size(max = 20) List<OffspringInput> offspring,
        @Size(max = 500) String notes) {
}
