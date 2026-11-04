package com.framehost.analytics.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record HeartbeatRequest(
        @NotNull(message = "Visitor ID is required")
        UUID visitorId
        ) {

}
