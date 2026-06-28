package com.ridehailing.booking_service.scheduler;


import com.ridehailing.booking_service.constants.RideStatus;
import com.ridehailing.booking_service.model.Booking;
import com.ridehailing.booking_service.repository.BookingRepository;
import com.ridehailing.booking_service.service.BookingService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@AllArgsConstructor
public class ScheduledBookingSweeper {

    private final BookingRepository bookingRepository;
    private final BookingService bookingService;

    @Scheduled(fixedRate = 2000) // Runs every 2 seconds
    @Transactional
    public void handleExpiredOffers() {
        // Find bookings stuck in "OFFERING" for more than 15 seconds
        Instant cutoffTime = Instant.now().minusSeconds(15);
        List<Booking> expiredOffers = bookingRepository.findExpiredOffers(String.valueOf(RideStatus.OFFERING), cutoffTime);

        for (Booking booking : expiredOffers) {
            // Treat an expiration exactly like a driver decline
            booking.getRejectedDrivers().add(booking.getDriverId());
            booking.setStatus(RideStatus.PENDING);
            booking.setDriverId(null);
            booking.setUpdatedAt(Instant.now());

            bookingRepository.save(booking);

            bookingService.persistOutBoxMessage(booking);
        }
    }

}
