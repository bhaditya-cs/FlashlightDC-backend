package org.flashlightdc.flashlight.dto;

public record BillStatsDto(
        long totalBills,
        long summarizedBills,
        long deltaLast24h
) {}