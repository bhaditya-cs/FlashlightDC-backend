package org.flashlightdc.flashlight.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.flashlightdc.flashlight.util.TermListDeserializer;


import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MemberDto(
        String bioguideId,
        String name,
        String partyName,
        String state,
        String district,
        @JsonDeserialize(using= TermListDeserializer.class)
        List<TermDto> terms,
        DepictionDto depiction,
        String url
) {}
