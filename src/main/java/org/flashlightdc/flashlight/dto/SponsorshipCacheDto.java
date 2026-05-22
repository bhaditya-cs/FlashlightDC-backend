package org.flashlightdc.flashlight.dto;

public record SponsorshipCacheDto(
        Long id,
        boolean byRequest,
        String memberName,
        String memberState,
        String memberParty,
        String memberBioguideId
) {}
