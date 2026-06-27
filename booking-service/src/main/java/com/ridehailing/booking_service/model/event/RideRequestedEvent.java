package com.ridehailing.booking_service.model.event;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.JoinColumn;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class RideRequestedEvent {
    private Long bookingId;
    private String riderId;
    private Double pickupLongitude;
    private Double pickupLatitude;
    private Double destinationLongitude;
    private Double destinationLatitude;
    private LocalDateTime requestedAt;

    @ElementCollection
    @CollectionTable(name = "booking_rejected_drivers", joinColumns = @JoinColumn(name = "booking_id"))
    @Column(name = "driver_id")
    private List<String> rejectedDrivers; //the blacklist

}