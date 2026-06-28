package com.ridehariling.fare_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@Slf4j
public class FareCalculationService {

    // In a reality, these would be loaded from a DB or config, possibly varied by city.
    private static final BigDecimal BASE_FARE = new BigDecimal("2.50");
    private static final BigDecimal PER_KM_RATE = new BigDecimal("1.25");
    private static final BigDecimal PER_MINUTE_RATE = new BigDecimal("0.15");

    public BigDecimal calculateFare(double distanceInKm, double durationInMinutes) {
        BigDecimal distance = BigDecimal.valueOf(distanceInKm);
        BigDecimal duration = BigDecimal.valueOf(durationInMinutes);

        BigDecimal distanceCost = distance.multiply(PER_KM_RATE);
        BigDecimal timeCost = duration.multiply(PER_MINUTE_RATE);

        BigDecimal totalFare = BASE_FARE.add(distanceCost).add(timeCost);

        // Round to 2 decimal places
        return totalFare.setScale(2, RoundingMode.HALF_UP);
    }
}
