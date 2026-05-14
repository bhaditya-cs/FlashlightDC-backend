package org.flashlightdc.flashlight.dto;

import java.math.BigDecimal;

public record CosponsorAveragesDto(
        BigDecimal avgCosponsorsPerBill,
        long maxCosponsors,
        long billsWithCosponsors,
        long totalBills,
        long bipartisanBills
) {}
