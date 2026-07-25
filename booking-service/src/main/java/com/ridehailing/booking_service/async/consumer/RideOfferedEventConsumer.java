package com.ridehailing.booking_service.async.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridehailing.booking_service.model.event.RideOfferedEvent;
import com.ridehailing.booking_service.service.BookingService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class RideOfferedEventConsumer {
    private final BookingService bookingService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "ride-offers", groupId = "booking-group")
    public void handleRideOfferedEventConsumer(String messagePayload){
        log.info("Received raw Kafka message: {}", messagePayload);

        try {
            RideOfferedEvent rideOfferedEvent = objectMapper.readValue(messagePayload, RideOfferedEvent.class);
            log.info("Successfully deserialized RideOfferedEvent for Booking ID: {}", rideOfferedEvent.getBookingId());
            bookingService.persistOfferedRide(rideOfferedEvent);

        } catch (Exception e) {
            log.error("Error occurred while processing RideOfferedEvent", e);
        }
    }
}
