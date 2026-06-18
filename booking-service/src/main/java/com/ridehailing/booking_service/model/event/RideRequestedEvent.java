package com.ridehailing.booking_service.model.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class RideRequestedEvent {
    private Long bookingId;
    private String riderId;
    private Double pickupLongitude;
    private Double pickupLatitude;
    private Double destinationLongitude;
    private Double destinationLatitude;
    private LocalDateTime requestedAt;

}