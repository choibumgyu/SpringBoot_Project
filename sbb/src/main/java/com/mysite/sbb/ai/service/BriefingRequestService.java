package com.mysite.sbb.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysite.sbb.ai.StockBriefingRequest;
import com.mysite.sbb.ai.dto.BriefingRequestStatusResponse;
import com.mysite.sbb.ai.dto.BriefingRequestSubmitResponse;
import com.mysite.sbb.ai.entity.BriefingRequest;
import com.mysite.sbb.ai.entity.OutboxEvent;
import com.mysite.sbb.ai.enums.BriefingStatus;
import com.mysite.sbb.ai.enums.OutboxEventType;
import com.mysite.sbb.ai.enums.OutboxStatus;
import com.mysite.sbb.ai.event.BriefingRequestedEventPayload;
import com.mysite.sbb.ai.repository.BriefingRequestRepository;
import com.mysite.sbb.ai.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BriefingRequestService {

    private static final String AGGREGATE_TYPE_BRIEFING_REQUEST = "BRIEFING_REQUEST";

    private final BriefingRequestRepository briefingRequestRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public BriefingRequestSubmitResponse submit(StockBriefingRequest req) {
        LocalDateTime now = LocalDateTime.now();

        String requestPayload = toJson(req);

        BriefingRequest briefingRequest = BriefingRequest.builder()
                .stockName(req.stockName())
                .currentPrice(toLong(req.currentPrice()))
                .prevClose(toLong(req.prevClose()))
                .requestPayload(requestPayload)
                .status(BriefingStatus.PENDING)
                .resultPayload(null)
                .errorMessage(null)
                .createdAt(now)
                .updatedAt(now)
                .build();

        BriefingRequest savedRequest = briefingRequestRepository.save(briefingRequest);

        BriefingRequestedEventPayload eventPayload = new BriefingRequestedEventPayload(
                savedRequest.getId(),
                savedRequest.getStockName(),
                now
        );

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .aggregateType(AGGREGATE_TYPE_BRIEFING_REQUEST)
                .aggregateId(savedRequest.getId())
                .eventType(OutboxEventType.BRIEFING_REQUESTED)
                .payload(toJson(eventPayload))
                .status(OutboxStatus.INIT)
                .createdAt(now)
                .publishedAt(null)
                .build();

        outboxEventRepository.save(outboxEvent);

        return new BriefingRequestSubmitResponse(
                savedRequest.getId(),
                savedRequest.getStatus()
        );
    }

    @Transactional(readOnly = true)
    public BriefingRequestStatusResponse getStatus(Long requestId) {
        BriefingRequest briefingRequest = briefingRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("BriefingRequest not found. id=" + requestId));

        return new BriefingRequestStatusResponse(
                briefingRequest.getId(),
                briefingRequest.getStatus(),
                briefingRequest.getResultPayload(),
                briefingRequest.getErrorMessage()
        );
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON serialization failed.", e);
        }
    }

    private Long toLong(Number value) {
        return value == null ? null : value.longValue();
    }
}