package com.ridehailing.location_service.service;

import com.ridehailing.location_service.model.request.DriverLocationRequest;
import com.ridehailing.location_service.model.response.DriverLocationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DriverLocationService {

    private final RedisTemplate<String, String> redisTemplate;

    // Two Redis keys — one for all driver positions, one for available-only
    private static final String GEO_ALL      = "driver:locations:all";
    private static final String GEO_AVAILABLE = "driver:locations:available";

    public void updateDriverLocation(DriverLocationRequest driverLocationRequest){
        Point point = new Point(driverLocationRequest.longitude(), driverLocationRequest.latitude()); // Redis: lng first

        // Always update the full location set
        redisTemplate.opsForGeo().add(GEO_ALL, point, driverLocationRequest.driverId());

        if (driverLocationRequest.available()) {
            // Driver is available — add to the available set
            redisTemplate.opsForGeo().add(GEO_AVAILABLE, point, driverLocationRequest.driverId());
        } else {
            // Driver went offline or started a ride — remove from available
            redisTemplate.opsForGeo().remove(GEO_AVAILABLE, driverLocationRequest.driverId());
        }

        // Set a TTL on the driver's presence key — if no ping for 30s, treat as offline
        redisTemplate.expire("driver:active:" + driverLocationRequest.driverId(),
                Duration.ofSeconds(30));
        redisTemplate.opsForValue().set("driver:active:" + driverLocationRequest.driverId(), "1");
    }


    public List<DriverLocationResponse> findNearbyDrivers( double latitude, double longitude, double radius){
        // call redis
        return Arrays.asList(new DriverLocationResponse());
    }
}
