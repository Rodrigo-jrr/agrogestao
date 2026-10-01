package com.agrogestao.livestock.dto;

import com.agrogestao.livestock.PregnancyResult;
import com.agrogestao.livestock.ReproductiveEventKind;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ReproductiveEventResponse(Long id, Long animalId, String animalTag, ReproductiveEventKind kind,
        LocalDate date, String sire, PregnancyResult pregnancyResult, LocalDate expectedCalvingDate,
        Long diagnosisEventId, Integer offspringCount, List<Long> offspringAnimalIds, String notes,
        LocalDateTime closedWithoutBirthAt) {
}
