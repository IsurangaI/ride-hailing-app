package com.ridehailing.booking_service.constants;

/**
 * Single source of truth for the topic names this service touches, shared by
 * KafkaTopicConfig (which declares them on the broker) and OutboxRelayWorker
 * (which routes outbox rows to them).
 */
public final class KafkaTopics {

    public static final String RIDE_REQUESTS = "ride-requests";
    public static final String TRIPS_COMPLETED = "trips-completed";
    public static final String RIDE_OFFERS = "ride-offers";

    private KafkaTopics() {
    }
}
