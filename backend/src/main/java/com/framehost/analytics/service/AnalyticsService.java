package com.framehost.analytics.service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.framehost.analytics.dto.AnalyticsStatsResponse;
import com.framehost.analytics.repository.AnalyticsEventRepository;
import com.framehost.analytics.repository.AnalyticsVisitorRepository;
import com.framehost.room.repository.InMemoryRoomRepository;

@Service
public class AnalyticsService {

    private static final Logger log
            = LoggerFactory.getLogger(AnalyticsService.class);

    private final AnalyticsVisitorRepository visitorRepository;
    private final AnalyticsEventRepository eventRepository;
    private final InMemoryRoomRepository roomRepository;

    public AnalyticsService(
            AnalyticsVisitorRepository visitorRepository,
            AnalyticsEventRepository eventRepository,
            InMemoryRoomRepository roomRepository
    ) {
        this.visitorRepository = visitorRepository;
        this.eventRepository = eventRepository;
        this.roomRepository = roomRepository;
    }

    @Transactional
    public void recordHeartbeat(UUID visitorId) {
        visitorRepository.recordHeartbeat(visitorId);
        visitorRepository.recordDailyActivity(visitorId);
    }

    // Returns aggregate statistics for the public dashboard.
    @Transactional(readOnly = true)
    public AnalyticsStatsResponse getPublicStats() {
        return new AnalyticsStatsResponse(
                visitorRepository.countTotalVisitors(),
                visitorRepository.countActiveVisitors(),
                visitorRepository.countDailyUniqueVisitors(),
                eventRepository.countRoomsCreated(),
                roomRepository.countActiveRooms()
        );
    }

    // Records room creation without interrupting room operations.
    public void recordRoomCreatedSafely() {
        try {
            eventRepository.recordRoomCreated();
        } catch (Exception exception) {
            log.warn(
                    "Unable to record room creation analytics event.",
                    exception
            );
        }
    }
}
