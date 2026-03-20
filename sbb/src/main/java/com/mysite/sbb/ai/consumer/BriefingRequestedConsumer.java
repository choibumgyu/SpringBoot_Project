package com.mysite.sbb.ai.consumer;

import com.mysite.sbb.ai.event.BriefingRequestedEventPayload;
import com.mysite.sbb.ai.service.BriefingConsumerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BriefingRequestedConsumer {

    private final BriefingConsumerService briefingConsumerService;

    @KafkaListener(
            topics = "${app.kafka.topic.briefing-requested}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(BriefingRequestedEventPayload eventPayload) {
        log.info("Kafka briefing requested event consumed. requestId={}, stockName={}",
                eventPayload.briefingRequestId(), eventPayload.stockName());

        briefingConsumerService.consume(eventPayload);
    }
}