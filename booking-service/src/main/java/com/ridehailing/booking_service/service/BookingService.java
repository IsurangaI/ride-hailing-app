package com.ridehailing.booking_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridehailing.booking_service.constants.RideStatus;
import com.ridehailing.booking_service.exception.BookingCreationException;
import com.ridehailing.booking_service.model.Booking;
import com.ridehailing.booking_service.model.OutboxMessage;
import com.ridehailing.booking_service.model.event.RideRequestedEvent;
import com.ridehailing.booking_service.model.request.BookingRequest;
import com.ridehailing.booking_service.repository.BookingRepository;
import com.ridehailing.booking_service.repository.OutboxMessagingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private static final String AGGREGATE_TYPE_BOOKING = "BOOKING";
    private static final String EVENT_TYPE_RIDE_REQUESTED = "RideRequestedEvent";

    private final BookingRepository bookingRepository;
    private final OutboxMessagingRepository outboxMessagingRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public String createBooking(BookingRequest bookingRequest) {
        try {
            Booking booking = Booking.builder()
                    .passengerId(bookingRequest.getPassengerId())
                    .pickupLongitude(bookingRequest.getPickupLongitude())
                    .pickupLatitude(bookingRequest.getPickupLatitude())
                    .destinationLongitude(bookingRequest.getDestinationLongitude())
                    .destinationLatitude(bookingRequest.getDestinationLatitude())
                    .status(RideStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();

            Booking savedBooking = bookingRepository.save(booking);

            RideRequestedEvent rideRequestedEvent = RideRequestedEvent.builder()
                    .bookingId(savedBooking.getId())
                    .riderId(savedBooking.getPassengerId())
                    .pickupLongitude(savedBooking.getPickupLongitude())
                    .pickupLatitude(savedBooking.getPickupLatitude())
                    .destinationLongitude(savedBooking.getDestinationLongitude())
                    .destinationLatitude(savedBooking.getDestinationLatitude())
                    .requestedAt(LocalDateTime.now())
                    .build();


            String jsonPayload = objectMapper.writeValueAsString(rideRequestedEvent);
            OutboxMessage outboxMessage = OutboxMessage.builder()
                    .aggregateType(AGGREGATE_TYPE_BOOKING)
                    .aggregateId(savedBooking.getId().toString())
                    .eventType(EVENT_TYPE_RIDE_REQUESTED)
                    .payload(jsonPayload)
                    .processed(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            outboxMessagingRepository.save(outboxMessage);
            log.info("Successfully recorded booking [{}] and outbox message concurrently.", savedBooking.getId());
            return savedBooking.getId().toString();
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize RideRequestedEvent for rider {}: {}", bookingRequest.getPassengerId(), e.getMessage());
            throw new BookingCreationException("Failed to process booking request due to serialization error.", e);
        } catch (Exception e) {
            log.error("Error occurred while recording booking for rider {}: {}", bookingRequest.getPassengerId(), e.getMessage(), e);
            throw new BookingCreationException("Failed to create booking for rider " + bookingRequest.getPassengerId(), e);
        }
    }
}