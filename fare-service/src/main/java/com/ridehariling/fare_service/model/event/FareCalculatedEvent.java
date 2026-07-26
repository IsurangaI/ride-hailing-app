package com.ridehariling.fare_service.model.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FareCalculatedEvent {
    private long bookingId;
    private String riderId;
    private BigDecimal finalFare;
}
