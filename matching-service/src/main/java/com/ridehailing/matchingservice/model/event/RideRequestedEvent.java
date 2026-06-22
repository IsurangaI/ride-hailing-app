package com.ridehailing.matchingservice.model.event;

import lombok.Data;
import java.util.UUID;

@Data
public class RideRequestedEvent {
    private Long bookingId;
    private String riderId;
    private double pickupLongitude;
    private double pickupLatitude;
    // Add any other fields you included in your outbox payload
}
