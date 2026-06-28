package com.ridehariling.fare_service.async.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridehariling.fare_service.model.Fare;
import com.ridehariling.fare_service.model.OutboxMessage;
import com.ridehariling.fare_service.model.event.FareCalculatedEvent;
import com.ridehariling.fare_service.model.event.TripCompletedEvent;
import com.ridehariling.fare_service.repository.FareRepository;
import com.ridehariling.fare_service.repository.OutboxMessagingRepository;
import com.ridehariling.fare_service.service.FareCalculationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class TripCompletedConsumer {

    private final ObjectMapper objectMapper;
    private final FareCalculationService fareCalculationService;
    private final FareRepository fareRepository;
    private final OutboxMessagingRepository outboxMessagingRepository; // Reusing your Outbox pattern!

    @KafkaListener(topics = "trips-completed", groupId = "fare-service-group")
    @Transactional
    public void consume(String payload) {
        try {
            TripCompletedEvent event = objectMapper.readValue(payload, TripCompletedEvent.class);

            // 1. Idempotency Check: Did we already calculate this?
            if (fareRepository.findByBookingId(event.getBookingId()).isPresent()) {
                log.warn("Fare already calculated for booking: {}", event.getBookingId());
                return;
            }

            // 2. Calculate the money
            BigDecimal finalFare = fareCalculationService.calculateFare(
                    event.getDistanceInKm(),
                    event.getDurationInMinutes()
            );

            // 3. Persist the Fare state
            Fare fare = new Fare(null, event.getBookingId(), event.getRiderId(), finalFare, "USD", "PENDING_PAYMENT");
            fareRepository.save(fare);

            // 4. Save to Outbox to emit 'fare-calculated'
            FareCalculatedEvent outboxPayload = new FareCalculatedEvent(
                    event.getBookingId(), event.getRiderId(), finalFare
            );


            OutboxMessage outboxMessage = OutboxMessage.builder()
                            .aggregateType("FARE")
                            .aggregateId(String.valueOf(event.getBookingId()))
                            .eventType("FARE_CALCULATED")
                            .payload(objectMapper.writeValueAsString(outboxPayload))
                            .processed(false)
                            .createdAt(LocalDateTime.now()).build();

            outboxMessagingRepository.save(outboxMessage);

            log.info("Successfully calculated fare of ${} for booking {}", finalFare, event.getBookingId());

        } catch (Exception e) {
            log.error("Failed to process trip completion: {}", payload, e);
            // Depending on configuration, Kafka will retry or send to a Dead Letter Queue (DLQ)
        }
    }
}
