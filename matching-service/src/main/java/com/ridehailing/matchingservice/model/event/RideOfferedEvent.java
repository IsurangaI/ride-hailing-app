package com.ridehailing.matchingservice.model.event;


import lombok.AllArgsConstructor;

@AllArgsConstructor
public class RideOfferedEvent {
    private Long bookingId;
    private String closestDriverId;
}
