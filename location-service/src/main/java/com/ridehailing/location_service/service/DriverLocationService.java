package com.ridehailing.location_service.service;

import com.ridehailing.location_service.model.request.DriverLocationRequest;
import com.ridehailing.location_service.exception.DriverRequestValidationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.domain.geo.Metrics;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DriverLocationService {

    private final RedisTemplate<String, String> redisTemplate;
    private final Validator validator; // Inject Validator

    // Two Redis keys — one for all driver positions, one for available-only
    private static final String GEO_ALL      = "driver:locations:all";
    private static final String GEO_AVAILABLE = "driver:locations:available";

    public void updateDriverLocation(String driverId, DriverLocationRequest driverLocationRequest){
        // Manual validation
        this.validateDriverLocationRequest(driverLocationRequest, driverId);
        Point point = new Point(driverLocationRequest.longitude(), driverLocationRequest.latitude()); // Redis: lng first

        // Always update the full location set
        redisTemplate.opsForGeo().add(GEO_ALL, point, driverId);

        if (driverLocationRequest.available()) {
            // Driver is available — add to the available set
            redisTemplate.opsForGeo().add(GEO_AVAILABLE, point, driverId);
        } else {
            // Driver went offline or started a ride — remove from available
            redisTemplate.opsForGeo().remove(GEO_AVAILABLE, driverId);
        }

        // Set a TTL on the driver's presence key — if no ping for 30s, treat as offline
        redisTemplate.expire("driver:active:" + driverId,
                Duration.ofSeconds(30));
        redisTemplate.opsForValue().set("driver:active:" + driverId, "1");
    }


    public List<String> findNearbyDrivers(double lat, double lng, double radiusKm) {
        // matching-service will also call this directly via Redis
        // but we expose it here too for debugging/admin use
        var circle = new Circle(
                new Point(lng, lat),
                new Distance(radiusKm, Metrics.KILOMETERS)
        );
        var args = RedisGeoCommands.GeoRadiusCommandArgs
                .newGeoRadiusArgs()
                .includeDistance()
                .sortAscending()
                .limit(10);

        var results = redisTemplate.opsForGeo()
                .radius(GEO_AVAILABLE, circle, args);

        if (results == null) return List.of();

        return results.getContent().stream()
                .map(r -> r.getContent().getName())
                .collect(Collectors.toList());
    }

    public void validateDriverLocationRequest(DriverLocationRequest driverLocationRequest, String driverId){
        Set<ConstraintViolation<DriverLocationRequest>> violations = validator.validate(driverLocationRequest);
        if (!violations.isEmpty()) {
            String errorMessage = violations.stream()
                    .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                    .collect(Collectors.joining(", "));
            throw new DriverRequestValidationException("Validation failed for DriverLocationRequest: " + errorMessage);
        }

        // Check if path variable driverId matches request body driverId
        if (!driverId.equals(driverLocationRequest.driverId())) {
            throw new DriverRequestValidationException("Driver ID not recognized.");
        }
    }
}