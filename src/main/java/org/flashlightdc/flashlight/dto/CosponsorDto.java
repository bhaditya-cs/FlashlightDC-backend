package org.flashlightdc.flashlight.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CosponsorDto(
        String bioguideId,
        String fullName,
        String party,
        String state,
        String district,
        String sponsorshipDate,
        boolean isOriginalCosponsor,
        String url
) {}
