package org.flashlightdc.flashlight.dto;

import java.util.List;
import java.util.Map;

public record BillStatsResponse(
        long totalBills,
        long billsThisMonth,
        long recentAmendments,
        Map<String, Long> chamberBreakdown,
        List<PopularTopic> popularTopics
) {
    public record PopularTopic(String policyArea, long count) {}
}
