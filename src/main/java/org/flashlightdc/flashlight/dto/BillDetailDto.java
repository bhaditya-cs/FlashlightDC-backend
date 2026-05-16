package org.flashlightdc.flashlight.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.flashlightdc.flashlight.util.CosponsorListDeserializer;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BillDetailDto(
        int congress,
        String number,
        String type,
        String title,
        String originChamber,
        String introducedDate,
        LatestActionDto latestAction,
        List<SponsorDto> sponsors,
        @JsonDeserialize(using = CosponsorListDeserializer.class)
        List<CosponsorDto> cosponsors,
        PolicyAreaDto policyArea,
        SummariesDto summaries,
        String constitutionalAuthorityStatementText,
        String url
) {}
