package com.ridehariling.fare_service.model.event;

import lombok.Data;

import java.util.UUID;

@Data
public class TripCompletedEvent {
    private long bookingId;
    private String riderId;
    private double distanceInKm;
    private double durationInMinutes;
}
