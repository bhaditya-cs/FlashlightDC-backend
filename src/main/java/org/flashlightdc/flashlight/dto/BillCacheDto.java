package org.flashlightdc.flashlight.dto;

import java.time.LocalDateTime;
import java.util.List;

public record BillCacheDto(
        Long id,
        Integer congress,
        String billNumber,
        String billType,
        String title,
        String originChamber,
        String introducedDate,
        String latestActionDate,
        String latestActionText,
        String policyArea,
        String url,
        String summary,
        LocalDateTime summaryUpdatedAt,
        LocalDateTime updatedAt,
        List<SponsorshipCacheDto> sponsors,
        List<CosponsorshipCacheDto> cosponsors
) {}
