package com.framehost.analytics.dto;

// Contains only aggregate statistics safe for public access.
public record AnalyticsStatsResponse(
        long totalVisitors,
        long activeVisitors,
        long dailyUniqueVisitors,
        long totalRoomsCreated,
        long activeRooms
        ) {

}
