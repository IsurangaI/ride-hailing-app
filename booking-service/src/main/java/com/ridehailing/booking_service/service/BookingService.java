package com.ridehailing.booking_service.service;

import com.ridehailing.booking_service.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {
    private final BookingRepository bookingRepository;

    @Autowired
    public BookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }
    @Transactional
    public String requestRide(String riderId, String pickupLocation, String dropoffLocation) {
        // write the booking to the bookings table
        //write to outbox table


        return riderId;
    }
}
