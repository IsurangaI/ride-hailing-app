package com.ridehailing.matchingservice.model.event;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class RideRequestedEvent {
    private Long bookingId;
    private String riderId;
    private double pickupLongitude;
    private double pickupLatitude;
    private List<String> rejectedDrivers; //the blacklist

}
