package org.flashlightdc.flashlight.dto;

import java.util.List;

public record BillSummaryDto(
        int congress,
        String number,
        String type,
        String title,
        String originChamber,
        String introducedDate,
        LatestActionDto latestAction,
        List<SponsorDto> sponsors,
        String url
) {}