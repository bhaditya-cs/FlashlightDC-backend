package org.flashlightdc.flashlight.dto;

public record TermCacheDto(
        Long id,
        String chamber,
        Integer startYear,
        Integer endYear
) {}
