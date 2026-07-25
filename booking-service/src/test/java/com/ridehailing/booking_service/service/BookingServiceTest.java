package com.ridehailing.booking_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridehailing.booking_service.model.Booking;
import com.ridehailing.booking_service.model.OutboxMessage;
import com.ridehailing.booking_service.model.request.BookingRequest;
import com.ridehailing.booking_service.repository.BookingRepository;
import com.ridehailing.booking_service.repository.OutboxMessagingRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class BookingServiceTest {

    private BookingService bookingService;

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private OutboxMessagingRepository outboxMessagingRepository;
    @Mock
    private ObjectMapper objectMapper;


    @BeforeEach
    void setUp() {
        this.bookingService = new BookingService(bookingRepository, outboxMessagingRepository, objectMapper);
    }


    @Test
    void createBookingShouldPersistBookingAndPersistOutboundMessage(){
        Booking booking = Booking.builder().id(123L).build();

        BookingRequest bookingRequest = BookingRequest.builder()
                .passengerId("PASSENGER_123")
                .pickupLongitude(6.777777)
                .pickupLatitude(8.999999)
                .destinationLongitude(10.58382)
                .destinationLatitude(11.9879382).build();

        Mockito.doReturn(booking).when(bookingRepository).save(any(Booking.class));
        String bookingId = bookingService.createBooking(bookingRequest);
        Assertions.assertEquals(booking.getId().toString(),bookingId);
        Mockito.verify(outboxMessagingRepository, Mockito.times(1)).save(any(OutboxMessage.class));

    }



}
