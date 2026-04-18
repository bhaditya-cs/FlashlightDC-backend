package org.flashlightdc.flashlight.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PaginationDto(
        int count,
        String next,
        String prev
) {}
