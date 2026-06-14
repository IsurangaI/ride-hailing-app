package com.ridehailing.location_service.controller;

import com.ridehailing.location_service.annontation.RequireRole;
import com.ridehailing.location_service.model.request.DriverLocationRequest;
import com.ridehailing.location_service.service.DriverLocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class DriverLocationController {
    private final DriverLocationService driverLocationService;

    @PostMapping("/ping")
    @RequireRole("DRIVER")
    public ResponseEntity<Void> updateLocation(
            @RequestHeader("X-User-Id") String driverId,
            @Valid @RequestBody DriverLocationRequest request) {
        driverLocationService.updateDriverLocation(driverId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<GeoResult<RedisGeoCommands.GeoLocation<String>>>> getNearbyDrivers(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "5.0") double radius) {
        return ResponseEntity.ok(driverLocationService.findNearbyDrivers(latitude, longitude, radius));
    }

}