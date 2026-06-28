package com.ridehariling.fare_service.async.producer;

import com.ridehariling.fare_service.model.OutboxMessage;
import com.ridehariling.fare_service.repository.OutboxMessagingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxRelayWorker {

    private final OutboxMessagingRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final String TOPIC_NAME = "fare-calculated";

    // Runs every 2000 milliseconds (2 seconds)
    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void publishOutboxMessages() {
        List<OutboxMessage> messages = outboxRepository.findByProcessedFalseOrderByIdAsc();

        if (messages.isEmpty()) {
            return; // Nothing to do, go back to sleep
        }

        log.info("Found {} unprocessed outbox messages. Initiating relay...", messages.size());

        for (OutboxMessage msg : messages) {
            try {
                // 1. Publish to Kafka
                // We use the aggregateId (Booking ID) as the Kafka Key to ensure events
                // for the same booking always land on the same Kafka partition.
                kafkaTemplate.send(TOPIC_NAME, msg.getAggregateId(), msg.getPayload())
                        .get(3, TimeUnit.SECONDS); // Block and wait for Kafka ACK

                // 2. Mark as processed ONLY if Kafka acknowledged the receipt
                msg.setProcessed(true);
                outboxRepository.save(msg);

                log.info("Successfully published outbox message ID: {} to Kafka", msg.getId());

            } catch (Exception e) {
                // If Kafka is down, or network fails, we log the error and BREAK the loop.
                // We do not process subsequent messages to preserve strict event ordering.
                log.error("Failed to publish outbox message ID: {}. Halting relay until next cycle.", msg.getId(), e);
                break;
            }
        }
    }
}
