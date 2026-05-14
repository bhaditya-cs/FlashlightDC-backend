package org.flashlightdc.flashlight.dto;

public record BillStatsDto(
        long totalBills,
        long deltaLast24h
) {}