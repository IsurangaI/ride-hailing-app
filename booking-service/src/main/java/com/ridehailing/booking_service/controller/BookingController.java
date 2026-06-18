package com.ridehailing.booking_service.controller;


import com.ridehailing.booking_service.model.Booking;
import com.ridehailing.booking_service.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("v1/bookings")
public class BookingController {

    private final BookingService bookingService;

    @Autowired
    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }


    @GetMapping
    public ResponseEntity<String> requestRide(@RequestBody Booking bookingRequest){
        bookingService.requestRide(bookingRequest.getRiderId(), bookingRequest.getPickupLocation(), bookingRequest.getDropoffLocation())
        return ResponseEntity.ok("Ride requested successfully");
    }


}
