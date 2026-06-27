package com.ridehailing.booking_service.async.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridehailing.booking_service.model.event.DriverMatchedEvent;
import com.ridehailing.booking_service.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DriverMatchedEventConsumer {

    private final BookingService bookingService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "driver-matches", groupId = "matching-group")
    public void handleDriverMatchedEvent(String messagePayload){
        log.info("Received raw Kafka message: {}", messagePayload);

        try {
            DriverMatchedEvent driverMatchedEvent = objectMapper.readValue(messagePayload, DriverMatchedEvent.class);
            log.info("Successfully deserialized DriverMatchedEvent for Booking ID: {}", driverMatchedEvent.getBookingId());
            bookingService.acceptBooking(driverMatchedEvent);
        } catch (Exception e) {
            log.error("Error occurred while processing DriverMatchedEvent", e);
        }
    }
}
