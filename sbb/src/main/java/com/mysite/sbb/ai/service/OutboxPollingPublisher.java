package com.mysite.sbb.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.mysite.sbb.ai.entity.OutboxEvent;
import com.mysite.sbb.ai.enums.OutboxEventType;
import com.mysite.sbb.ai.enums.OutboxStatus;
import com.mysite.sbb.ai.event.BriefingRequestedEventPayload;
import com.mysite.sbb.ai.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPollingPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, BriefingRequestedEventPayload> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.kafka.topic.briefing-requested}")
    private String briefingRequestedTopic;

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents =
                outboxEventRepository.findTop10ByStatusOrderByCreatedAtAsc(OutboxStatus.INIT);

        if (pendingEvents.isEmpty()) {
            return;
        }

        for (OutboxEvent event : pendingEvents) {
            try {
                publishSingleEvent(event);
                event.markPublished();

                log.info("Outbox event published. outboxEventId={}, aggregateId={}, eventType={}",
                        event.getId(), event.getAggregateId(), event.getEventType());

            } catch (Exception e) {
                event.markFailed();

                log.error("Outbox event publish failed. outboxEventId={}, aggregateId={}, eventType={}",
                        event.getId(), event.getAggregateId(), event.getEventType(), e);
            }
        }
    }

    private void publishSingleEvent(OutboxEvent event) throws Exception {
        if (event.getEventType() == OutboxEventType.BRIEFING_REQUESTED) {
            BriefingRequestedEventPayload payload =
                    objectMapper.readValue(event.getPayload(), BriefingRequestedEventPayload.class);

            kafkaTemplate.send(
                    briefingRequestedTopic,
                    String.valueOf(event.getAggregateId()),
                    payload
            ).get();

            return;
        }

        throw new IllegalArgumentException("Unsupported outbox event type: " + event.getEventType());
    }
}