package org.flashlightdc.flashlight.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.flashlightdc.flashlight.util.YesNoDeserializer;


@JsonIgnoreProperties(ignoreUnknown = true)
public record SponsorDto(
        String bioguideId,
        String fullName,
        String party,
        String state,
        String district,
        @JsonDeserialize(using = YesNoDeserializer.class)
        boolean isByRequest,
        String url
) {}
