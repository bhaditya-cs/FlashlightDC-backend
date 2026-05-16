package org.flashlightdc.flashlight.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.flashlightdc.flashlight.util.CosponsorListDeserializer;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CosponsorListResponse(
        List<CosponsorDto> cosponsors,
        PaginationDto pagination
) {}
