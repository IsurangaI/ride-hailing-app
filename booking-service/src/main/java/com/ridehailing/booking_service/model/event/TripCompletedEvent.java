package com.ridehailing.booking_service.model.event;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TripCompletedEvent extends Event {
    private Long bookingId;
    private String riderId;
    private double distanceInKm;
    private double durationInMinutes;


    @Override
    public String getEventType() {
        return "TripCompletedEvent";
    }
}
