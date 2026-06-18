package com.ridehailing.booking_service.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String aggregateType; // e.g., "BOOKING"
    private String aggregateId;   // e.g., Booking ID string
    private String eventType;     // e.g., "RideRequestedEvent"

    @Column(columnDefinition = "TEXT")
    private String payload;       // Serialized JSON event object

    private boolean processed;
    private LocalDateTime createdAt;
}
