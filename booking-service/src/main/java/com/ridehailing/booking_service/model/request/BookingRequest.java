package com.ridehailing.booking_service.model.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BookingRequest {
    private String passengerId;
    private Double pickupLongitude;
    private Double pickupLatitude;
    private Double destinationLongitude;
    private Double destinationLatitude;
}