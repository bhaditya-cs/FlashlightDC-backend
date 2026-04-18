package org.flashlightdc.flashlight.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CosponsorsDto(
        int count,
        int countIncludingWithdrawnCosponsors,
        String url
) {}
