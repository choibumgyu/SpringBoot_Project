package com.mysite.sbb.ai.event;

import java.time.LocalDateTime;

public record BriefingRequestedEventPayload(
        Long briefingRequestId,
        String stockName,
        LocalDateTime requestedAt
) {
}