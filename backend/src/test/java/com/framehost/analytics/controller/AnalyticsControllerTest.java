package com.framehost.analytics.controller;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.framehost.analytics.dto.AnalyticsStatsResponse;
import com.framehost.analytics.service.AnalyticsService;
import com.framehost.common.GlobalExceptionHandler;

@WebMvcTest(AnalyticsController.class)
@Import(GlobalExceptionHandler.class)
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnalyticsService analyticsService;

    @Test
    void shouldReturn204WhenHeartbeatIsValid() throws Exception {

        UUID visitorId = UUID.randomUUID();

        mockMvc.perform(
                post("/api/analytics/heartbeat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "visitorId": "%s"
                                }
                                """.formatted(visitorId))
        )
                .andExpect(status().isNoContent());

        verify(analyticsService).recordHeartbeat(visitorId);
    }

    @Test
    void shouldReturn400WhenVisitorIdIsMissing() throws Exception {

        mockMvc.perform(
                post("/api/analytics/heartbeat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
        )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenVisitorIdIsInvalid() throws Exception {

        mockMvc.perform(
                post("/api/analytics/heartbeat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "visitorId": "invalid-uuid"
                                }
                                """)
        )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnPublicAnalyticsStats() throws Exception {

        // Prepare example aggregate statistics.
        AnalyticsStatsResponse stats = new AnalyticsStatsResponse(
                120,
                15,
                35,
                80,
                4
        );

        // Mock the service response.
        when(analyticsService.getPublicStats()).thenReturn(stats);

        // Call the public statistics endpoint.
        mockMvc.perform(get("/api/analytics/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVisitors").value(120))
                .andExpect(jsonPath("$.activeVisitors").value(15))
                .andExpect(jsonPath("$.dailyUniqueVisitors").value(35))
                .andExpect(jsonPath("$.totalRoomsCreated").value(80))
                .andExpect(jsonPath("$.activeRooms").value(4));

        // Verify that the service was called.
        verify(analyticsService).getPublicStats();
    }
}
