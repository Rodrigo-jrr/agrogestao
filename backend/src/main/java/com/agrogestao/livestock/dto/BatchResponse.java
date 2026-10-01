package com.agrogestao.livestock.dto;

import com.agrogestao.livestock.BatchTracking;

public record BatchResponse(Long id, String name, Long speciesId, String speciesName, BatchTracking tracking,
        int headCount, Long activityId, String activityName, String notes, boolean archived,
        java.time.LocalDate withdrawalUntil) {
}
