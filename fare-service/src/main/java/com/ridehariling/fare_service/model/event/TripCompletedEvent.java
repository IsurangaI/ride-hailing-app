package com.ridehariling.fare_service.model.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TripCompletedEvent {
    private Long bookingId;
    private String riderId;
    private double distanceInKm;
    private double durationInMinutes;
}
