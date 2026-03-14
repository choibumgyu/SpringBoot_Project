package com.mysite.sbb.ai.dto;

import com.mysite.sbb.ai.enums.BriefingStatus;

public record BriefingRequestSubmitResponse(
        Long requestId,
        BriefingStatus status
) {
}