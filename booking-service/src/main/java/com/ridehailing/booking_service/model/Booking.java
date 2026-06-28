package com.ridehailing.booking_service.model;

import com.ridehailing.booking_service.constants.RideStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

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
    @ElementCollection
    @CollectionTable(name = "booking_rejected_drivers", joinColumns = @JoinColumn(name = "booking_id"))
    @Column(name = "driver_id")
    private List<String> rejectedDrivers; //the blacklist
}
