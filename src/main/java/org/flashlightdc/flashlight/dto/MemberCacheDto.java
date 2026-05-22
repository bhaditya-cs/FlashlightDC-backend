package org.flashlightdc.flashlight.dto;

import java.util.List;

public record MemberCacheDto(
        String bioguideId,
        String name,
        String partyName,
        String state,
        String district,
        String imageUrl,
        String attribution,
        String url,
        List<TermCacheDto> terms,
        List<SponsorshipCacheDto> sponsorships,
        List<CosponsorshipCacheDto> cosponsorships
) {}
