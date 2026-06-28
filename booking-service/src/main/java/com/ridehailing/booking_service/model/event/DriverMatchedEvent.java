package com.ridehailing.booking_service.model.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DriverMatchedEvent extends Event {

    private Long bookingId;
    private String driverId;
    private String status;

    @Override
    public String getEventType() {
        return "DriverMatchedEvent";
    }
}
