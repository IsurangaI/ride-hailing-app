package com.ridehailing.booking_service.model.event;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TripCompletedEvent extends Event {
    private Long bookingId;
    private String driverId;
    private String passengerId;
    private LocalDateTime completedAt;


    @Override
    public String getEventType() {
        return "TripCompletedEvent";
    }
}
