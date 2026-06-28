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


    @PostMapping("/{id}/accept")
    public ResponseEntity<String> acceptBooking(@PathVariable Long bookingId,@RequestBody String driverId) {
        bookingService.acceptBooking(bookingId,driverId);
        return ResponseEntity.ok("Booking accepted successfully.");
    }

    @PostMapping("/{id}/decline")
    public ResponseEntity<String> declineBooking(@PathVariable Long bookingId,@RequestBody String driverId) {
        bookingService.declineBooking(bookingId, driverId);
        return ResponseEntity.ok("Booking declined successfully.");
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<String> startBooking(@PathVariable Long id) {
        bookingService.startBooking(id);
        return ResponseEntity.ok("Booking started successfully.");
    }

    @PostMapping("/{id}/end")
    public ResponseEntity<String> endBooking(@PathVariable Long id) {
        bookingService.endBooking(id);
        return ResponseEntity.ok("Booking ended successfully.");
    }

}