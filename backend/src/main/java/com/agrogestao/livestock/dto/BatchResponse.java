package com.agrogestao.livestock.dto;

import com.agrogestao.livestock.BatchTracking;
import com.agrogestao.livestock.Species;

public record BatchResponse(Long id, String name, Species species, BatchTracking tracking, int headCount,
        Long activityId, String activityName, String notes, boolean archived, java.time.LocalDate withdrawalUntil) {
}
