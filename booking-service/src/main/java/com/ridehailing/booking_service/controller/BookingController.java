package com.ridehailing.booking_service.controller;


import com.ridehailing.booking_service.constants.RideStatus;
import com.ridehailing.booking_service.model.request.BookingRequest;
import com.ridehailing.booking_service.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<String> createBooking(@RequestBody BookingRequest bookingRequest) {
        String bookingId = bookingService.createBooking(bookingRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RideStatus> pollBookingStatus(@PathVariable Long id) {
        RideStatus status = bookingService.getBookingStatus(id);
        return ResponseEntity.ok(status);
    }





}