package com.mysite.sbb.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysite.sbb.ai.StockBriefingRequest;
import com.mysite.sbb.ai.entity.BriefingRequest;
import com.mysite.sbb.ai.enums.BriefingStatus;
import com.mysite.sbb.ai.event.BriefingRequestedEventPayload;
import com.mysite.sbb.ai.repository.BriefingRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BriefingConsumerService {

    private final BriefingRequestRepository briefingRequestRepository;
    private final StockBriefingService stockBriefingService;
    private final ObjectMapper objectMapper;

    @Transactional
    public void consume(BriefingRequestedEventPayload eventPayload) {

        Long requestId = eventPayload.briefingRequestId();
        BriefingRequest briefingRequest = null;

        try {
            briefingRequest = briefingRequestRepository.findById(requestId)
                    .orElseThrow(() -> new IllegalArgumentException("BriefingRequest not found. id=" + requestId));

            if (briefingRequest.getStatus() == BriefingStatus.COMPLETED) {
                log.info("이미 완료된 요청입니다. requestId={}", requestId);
                return;
            }

            if (briefingRequest.getStatus() == BriefingStatus.PROCESSING) {
                log.info("이미 처리 중인 요청입니다. requestId={}", requestId);
                return;
            }

            briefingRequest.markProcessing(LocalDateTime.now());

            StockBriefingRequest req =
                    objectMapper.readValue(briefingRequest.getRequestPayload(), StockBriefingRequest.class);

            Map<String, Object> result = stockBriefingService.generate(req);
            String resultPayloadJson = objectMapper.writeValueAsString(result);

            briefingRequest.markCompleted(resultPayloadJson, LocalDateTime.now());

            log.info("briefing completed. requestId={}, stockName={}",
                    briefingRequest.getId(), briefingRequest.getStockName());

        } catch (Exception e) {

            if (briefingRequest != null) {
                briefingRequest.markFailed(truncateErrorMessage(e.getMessage()), LocalDateTime.now());
            }

            log.error("briefing failed. requestId={}", requestId, e);
        }
    }
    private String truncateErrorMessage(String message) {
        if (message == null) {
            return "unknown error";
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}