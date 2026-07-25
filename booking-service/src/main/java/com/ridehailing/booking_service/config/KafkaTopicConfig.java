package com.ridehailing.booking_service.config;

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
    public NewTopic rideRequestsTopic() {
        return TopicBuilder.name("ride-requests")
                .partitions(3)  // Allows up to 3 matching-service instances in 'matching-group'
                .replicas(1)    // 1 for local Docker (single broker); 3 for prod
                .build();
    }

    @Bean
    public NewTopic tripsCompletedTopic() {
        return TopicBuilder.name("trips-completed")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
