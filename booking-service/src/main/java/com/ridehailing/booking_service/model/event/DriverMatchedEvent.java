package com.ridehailing.booking_service.model.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DriverMatchedEvent {
    private Long bookingId;
    private String driverId;
    private String status;
}
