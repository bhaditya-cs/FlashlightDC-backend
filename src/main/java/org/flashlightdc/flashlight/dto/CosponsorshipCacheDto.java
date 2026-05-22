package org.flashlightdc.flashlight.dto;

public record CosponsorshipCacheDto(
        Long id,
        boolean original,
        String sponsoredDate,
        String memberName,
        String memberState,
        String memberParty,
        String memberBioguideId
) {}
