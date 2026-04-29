package org.flashlightdc.flashlight.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SummaryItemDto(
        String text,
        String updateDate,
        String actionDate
) {}
