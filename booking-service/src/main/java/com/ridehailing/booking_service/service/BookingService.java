package com.ridehailing.booking_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridehailing.booking_service.constants.RideStatus;
import com.ridehailing.booking_service.exception.BookingCreationException;
import com.ridehailing.booking_service.exception.BookingNotFoundException;
import com.ridehailing.booking_service.model.Booking;
import com.ridehailing.booking_service.model.OutboxMessage;
import com.ridehailing.booking_service.model.event.Event;
import com.ridehailing.booking_service.model.event.RideOfferedEvent;
import com.ridehailing.booking_service.model.event.RideRequestedEvent;
import com.ridehailing.booking_service.model.event.TripCompletedEvent;
import com.ridehailing.booking_service.model.request.BookingRequest;
import com.ridehailing.booking_service.repository.BookingRepository;
import com.ridehailing.booking_service.repository.OutboxMessagingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private static final String AGGREGATE_TYPE_BOOKING = "BOOKING";

    private final BookingRepository bookingRepository;
    private final OutboxMessagingRepository outboxMessagingRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public String createBooking(BookingRequest bookingRequest) {
        try {
            Booking booking = Booking.builder().passengerId(bookingRequest.getPassengerId()).pickupLongitude(bookingRequest.getPickupLongitude()).pickupLatitude(bookingRequest.getPickupLatitude()).destinationLongitude(bookingRequest.getDestinationLongitude()).destinationLatitude(bookingRequest.getDestinationLatitude()).status(RideStatus.PENDING).createdAt(LocalDateTime.now()).build();

            Booking savedBooking = bookingRepository.save(booking);

            RideRequestedEvent rideRequestedEvent = RideRequestedEvent.builder().bookingId(savedBooking.getId()).riderId(savedBooking.getPassengerId()).pickupLongitude(savedBooking.getPickupLongitude()).pickupLatitude(savedBooking.getPickupLatitude()).destinationLongitude(savedBooking.getDestinationLongitude()).destinationLatitude(savedBooking.getDestinationLatitude()).requestedAt(LocalDateTime.now()).rejectedDrivers(savedBooking.getRejectedDrivers()).build();

            this.persistOutBoxMessage(savedBooking, rideRequestedEvent);

            return savedBooking.getId().toString();
        } catch (Exception e) {
            log.error("Error occurred while recording booking for rider {}: {}", bookingRequest.getPassengerId(), e.getMessage(), e);
            throw new BookingCreationException("Failed to create booking for rider " + bookingRequest.getPassengerId(), e);
        }
    }


    public void persistOfferedRide(RideOfferedEvent rideOfferedEvent) {
        try {
            Optional<Booking> booking = bookingRepository.findById(rideOfferedEvent.getBookingId());
            booking.ifPresentOrElse(b -> {
                b.setStatus(RideStatus.OFFERING);
                b.setDriverId(rideOfferedEvent.getDriverId());
                b.setUpdatedAt(Instant.now());
                bookingRepository.save(b);
            }, RuntimeException::new);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public RideStatus getBookingStatus(Long id) {
        return bookingRepository.findById(id).map(Booking::getStatus).orElseThrow(() -> {
            log.error("Booking [{}] not found.", id);
            return new BookingNotFoundException("Booking not found with id: " + id);
        });
    }

    @Transactional
    public void acceptBooking(Long bookingId, String driverId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();

        // Concurrency Check: Ensure it wasn't cancelled or timed out!
        if (!booking.getStatus().equals(RideStatus.OFFERING) || !booking.getDriverId().equals(driverId)) {
            throw new IllegalStateException("Offer is no longer valid");
        }

        booking.setStatus(RideStatus.ACCEPTED);
        bookingRepository.save(booking);

        //need to drop an event and notify the rider and implement the downstream process
    }

    @Transactional
    public void declineBooking(Long bookingId, String driverId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();

        // Add driver to blacklist
        booking.getRejectedDrivers().add(driverId);
        booking.setStatus(RideStatus.PENDING); // Revert to pending
        booking.setDriverId(null);
        Booking savedBooking = bookingRepository.save(booking);
        // FIRE THE SAGA AGAIN: Drop a new RideRequestedEvent into the Outbox
        // so the Matching Service finds the NEXT nearest driver.
        RideRequestedEvent rideRequestedEvent = RideRequestedEvent.builder().bookingId(booking.getId()).riderId(booking.getPassengerId()).pickupLongitude(booking.getPickupLongitude()).pickupLatitude(booking.getPickupLatitude()).destinationLongitude(booking.getDestinationLongitude()).destinationLatitude(booking.getDestinationLatitude()).requestedAt(LocalDateTime.now()).rejectedDrivers(booking.getRejectedDrivers()).build();
        this.persistOutBoxMessage(savedBooking, rideRequestedEvent);
    }

    @Transactional
    public void startBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + bookingId));

        if (!booking.getStatus().equals(RideStatus.ACCEPTED)) {
            throw new IllegalStateException("Booking can only be started if its status is ACCEPTED. Current status: " + booking.getStatus());
        }

        booking.setStatus(RideStatus.IN_PROGRESS);
        booking.setStartedAt(LocalDateTime.now());
        bookingRepository.save(booking);
        log.info("Booking [{}] started successfully.", bookingId);
    }

    @Transactional
    public void endBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + bookingId));

        if (!booking.getStatus().equals(RideStatus.IN_PROGRESS)) {
            throw new IllegalStateException("Booking can only be ended if its status is IN_PROGRESS. Current status: " + booking.getStatus());
        }

        booking.setStatus(RideStatus.COMPLETED);
        booking.setCompletedAt(LocalDateTime.now());
        bookingRepository.save(booking);
        log.info("Booking [{}] ended successfully.", bookingId);

        try {
            TripCompletedEvent tripCompletedEvent = TripCompletedEvent.builder().bookingId(booking.getId()).driverId(booking.getDriverId()).passengerId(booking.getPassengerId()).completedAt(LocalDateTime.now()).build();

            String jsonPayload = objectMapper.writeValueAsString(tripCompletedEvent);
            OutboxMessage outboxMessage = OutboxMessage.builder().aggregateType(AGGREGATE_TYPE_BOOKING).aggregateId(booking.getId().toString()).eventType(tripCompletedEvent.getEventType()).payload(jsonPayload).processed(false).createdAt(LocalDateTime.now()).build();
            outboxMessagingRepository.save(outboxMessage);
            log.info("Successfully recorded TripCompletedEvent for booking [{}].", booking.getId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize TripCompletedEvent for booking {}: {}", booking.getId(), e.getMessage());
            throw new RuntimeException("Failed to process trip completion due to serialization error.", e);
        }
    }


    public void persistOutBoxMessage(Booking booking, Event event) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(event);
            OutboxMessage outboxMessage = OutboxMessage.builder().aggregateType(AGGREGATE_TYPE_BOOKING).aggregateId(booking.getId().toString()).eventType(event.getEventType()).payload(jsonPayload).processed(false).createdAt(LocalDateTime.now()).build();
            outboxMessagingRepository.save(outboxMessage);
            log.info("Successfully recorded booking [{}] and outbox message concurrently.", booking.getId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize RideRequestedEvent for rider {}: {}", booking.getPassengerId(), e.getMessage());
            throw new BookingCreationException("Failed to process booking request due to serialization error.", e);
        }
    }

}