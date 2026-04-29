package org.flashlightdc.flashlight.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TextFormatDto(
        String type,
        String url
) {}
