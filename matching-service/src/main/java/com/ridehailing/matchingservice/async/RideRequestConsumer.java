package com.ridehailing.matchingservice.async;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridehailing.matchingservice.model.event.RideRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RideRequestConsumer {

    private final ObjectMapper objectMapper;
    // private final MatchingEngineService matchingEngineService; <-- We will build this next

    @KafkaListener(topics = "ride-requests", groupId = "matching-group")
    public void handleRideRequest(String messagePayload) {
        log.info("Received raw Kafka message: {}", messagePayload);

        try {
            // 1. Deserialize the JSON payload
            RideRequestedEvent event = objectMapper.readValue(messagePayload, RideRequestedEvent.class);
            log.info("Successfully deserialized request for Booking ID: {}", event.getBookingId());

            // 2. Trigger the matching engine (Placeholder for now)
            // matchingEngineService.findAndAssignDriver(event);

        } catch (Exception e) {
            // If the JSON is malformed, we catch it here so the consumer doesn't enter an infinite crash loop
            log.error("Failed to process ride request event. Payload: {}", messagePayload, e);
        }
    }
}