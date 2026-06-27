package com.ridehailing.booking_service.model;

import com.ridehailing.booking_service.constants.RideStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String passengerId;
    private Double pickupLongitude;
    private Double pickupLatitude;
    private Double destinationLongitude;
    private Double destinationLatitude;

    @Enumerated(EnumType.STRING)
    private RideStatus status; // PENDING, ACCEPTED, REJECTED, COMPLETED

    private LocalDateTime createdAt;
    private String driverId;
}
