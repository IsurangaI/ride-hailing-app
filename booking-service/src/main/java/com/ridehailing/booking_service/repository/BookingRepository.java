package com.ridehailing.booking_service.repository;


import com.ridehailing.booking_service.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    //JPQL method
    @Query("SELECT b FROM Booking b WHERE b.status = :status AND b.updatedAt < :cutoffTime")
    List<Booking> findExpiredOffers(
            @Param("status") String status,
            @Param("cutoffTime") Instant cutoffTime
    );

    //JPA method
    List<Booking> findByStatusAndUpdatedAtBefore(String status, Instant cutoffTime);

}
