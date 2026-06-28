package com.ridehariling.fare_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "fares")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Fare {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private long bookingId;

    @Column(nullable = false)
    private String riderId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    private String currency = "USD";

    @Column(nullable = false)
    private String status; // e.g., PENDING_PAYMENT, PAID
}
