package com.ridehailing.booking_service.repository;


import com.ridehailing.booking_service.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {


    //@Transactional
    // write the booking to the bookings table
    //write to outbox table

}
