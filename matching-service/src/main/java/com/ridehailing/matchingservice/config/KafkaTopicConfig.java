package com.ridehailing.matchingservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares the topics this service PRODUCES as code, so a fresh broker gets them
 * on startup without relying on the broker's auto-create defaults (1 partition, RF 1).
 * KafkaAdmin picks up every NewTopic bean at boot and creates whatever is missing.
 */
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic rideOffersTopic() {
        return TopicBuilder.name("ride-offers")
                .partitions(3)  // Allows up to 3 booking-service instances in 'booking-group'
                .replicas(1)    // 1 for local Docker (single broker); 3 for prod
                .build();
    }
}
