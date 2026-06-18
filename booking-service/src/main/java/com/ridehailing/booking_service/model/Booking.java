package com.ridehailing.booking_service.model;

import com.ridehailing.booking_service.constants.RideStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
@Entity(name = "bookings")
public class Booking {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private String id;
    private String riderId;
    private String pickupLocation;
    private String dropoffLocation;
    private RideStatus status;
}
