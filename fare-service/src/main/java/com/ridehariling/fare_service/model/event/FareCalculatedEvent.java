package com.ridehariling.fare_service.model.event;

import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@AllArgsConstructor
public class FareCalculatedEvent {
    private long bookingId;
    private String riderId;
    private BigDecimal finalFare;
}
