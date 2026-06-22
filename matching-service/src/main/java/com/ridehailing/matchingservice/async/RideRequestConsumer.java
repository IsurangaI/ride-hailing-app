package com.ridehailing.matchingservice.async;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridehailing.matchingservice.model.event.RideRequestedEvent;
import com.ridehailing.matchingservice.service.MatchingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RideRequestConsumer {

    private final MatchingService matchingService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "ride-requests", groupId = "matching-group")
    public void handleRideRequest(String messagePayload) {
        log.info("Received raw Kafka message: {}", messagePayload);

        try {
            RideRequestedEvent event = objectMapper.readValue(messagePayload, RideRequestedEvent.class);
            log.info("Successfully deserialized request for Booking ID: {}", event.getBookingId());

            matchingService.findAndAssignDriver(event);
        } catch (Exception e) {
            log.error("Failed to process ride request event. Payload: {}", messagePayload, e);
        }
    }
}