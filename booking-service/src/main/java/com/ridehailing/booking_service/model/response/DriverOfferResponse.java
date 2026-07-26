package com.ridehailing.booking_service.model.response;

import com.ridehailing.booking_service.constants.RideStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class DriverOfferResponse {
    private Long bookingId;
    private String passengerId;
    private Double pickupLongitude;
    private Double pickupLatitude;
    private Double destinationLongitude;
    private Double destinationLatitude;
    private RideStatus status;
    private Instant offeredAt;
}
