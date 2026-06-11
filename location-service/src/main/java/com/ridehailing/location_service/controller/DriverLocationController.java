package com.ridehailing.location_service.controller;

import com.ridehailing.location_service.model.request.DriverLocationRequest;
import com.ridehailing.location_service.model.response.DriverLocationResponse;
import com.ridehailing.location_service.service.DriverLocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
public class DriverLocationController {
    private final DriverLocationService driverLocationService;

    @PostMapping("/ping")
    public ResponseEntity<Void> updateLocation(
            @RequestHeader("X-User-Id") Long driverId,
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody DriverLocationRequest request) {
        // Assuming DriverLocationRequest needs to be updated to include driverId,
        // or the service method needs to be updated to take driverId as a separate parameter.
        // For now, I'll pass driverId to the service.
        driverLocationService.updateDriverLocation(driverId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<String>> getNearbyDrivers(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "5.0") double radius) {
        return ResponseEntity.ok(driverLocationService.findNearbyDrivers(latitude, longitude, radius));
    }

}