package com.framehost.analytics.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.framehost.analytics.dto.AnalyticsStatsResponse;
import com.framehost.analytics.dto.HeartbeatRequest;
import com.framehost.analytics.service.AnalyticsService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    // POST /api/analytics/heartbeat
    @PostMapping("/heartbeat")
    public ResponseEntity<Void> recordHeartbeat(
            @Valid @RequestBody HeartbeatRequest request
    ) {

        analyticsService.recordHeartbeat(request.visitorId());

        return ResponseEntity.noContent().build();
    }
    // GET /api/analytics/stats

    @GetMapping("/stats")
    public ResponseEntity<AnalyticsStatsResponse> getPublicStats() {
        return ResponseEntity.ok(analyticsService.getPublicStats());
    }
}
