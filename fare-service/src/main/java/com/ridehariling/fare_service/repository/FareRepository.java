package com.ridehariling.fare_service.repository;

import com.ridehariling.fare_service.model.Fare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FareRepository extends JpaRepository<Fare, UUID> {
    java.util.Optional<Fare> findByBookingId(long bookingId);
}
