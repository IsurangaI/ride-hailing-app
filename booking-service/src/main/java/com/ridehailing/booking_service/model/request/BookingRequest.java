package com.ridehailing.booking_service.model.request;

import lombok.Data;

@Data
public class BookingRequest {
    private String passengerId;
    private Double pickupLongitude;
    private Double pickupLatitude;
    private Double destinationLongitude;
    private Double destinationLatitude;
}