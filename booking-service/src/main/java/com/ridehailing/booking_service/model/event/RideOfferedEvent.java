package com.ridehailing.booking_service.model.event;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RideOfferedEvent {
    private Long bookingId;
    private String driverId;
}
