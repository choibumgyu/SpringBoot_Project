package com.mysite.sbb.ai.dto;

import com.mysite.sbb.ai.enums.BriefingStatus;

public record BriefingRequestStatusResponse(
        Long requestId,
        BriefingStatus status,
        String resultPayload,
        String errorMessage
) {
}