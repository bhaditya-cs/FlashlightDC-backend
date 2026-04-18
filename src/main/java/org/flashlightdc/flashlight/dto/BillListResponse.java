package org.flashlightdc.flashlight.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BillListResponse(
        List<BillSummaryDto> bills,
        PaginationDto pagination
) {}