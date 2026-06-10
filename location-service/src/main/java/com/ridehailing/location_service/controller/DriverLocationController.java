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
    public ResponseEntity<Void> updateLocation(@Valid @RequestBody DriverLocationRequest request) {
        driverLocationService.updateDriverLocation(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<DriverLocationResponse>> getNearbyDrivers(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "5.0") double radius) {
        return ResponseEntity.ok(driverLocationService.findNearbyDrivers(latitude, longitude, radius));
    }

}
