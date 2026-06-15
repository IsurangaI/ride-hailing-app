package com.ridehailing.location_service.service;

import ch.hsr.geohash.GeoHash;
import com.ridehailing.location_service.model.request.DriverLocationRequest;
import com.ridehailing.location_service.exception.DriverRequestValidationException;
import com.ridehailing.location_service.util.GeoUtils;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.domain.geo.Metrics;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
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
    private static final String GEO_AVAILABLE = "driver:locations:available"; // This key will no longer be used for sharded queries

    public void updateDriverLocation(String driverId, DriverLocationRequest driverLocationRequest) {
        double longitude = driverLocationRequest.longitude();
        double latitude = driverLocationRequest.latitude();

        validateDriverLocationRequest(driverLocationRequest, driverId);
        // 1. Resolve the region dynamically based on coordinates
        String shardedKey = GeoUtils.getAutomaticShardKey(longitude, latitude);

        // 3. Write to the specific regional shard
        Point location = new Point(longitude, latitude);
        redisTemplate.opsForGeo().add(shardedKey, location, driverId);
        // Also add to GEO_ALL for general queries (if needed, otherwise remove)
        redisTemplate.opsForGeo().add(GEO_ALL, location, driverId);
    }


    public List<GeoResult<RedisGeoCommands.GeoLocation<String>>> findNearbyDrivers(double riderLng, double riderLat, double radiusInKm) {
        // Define the precision for geohashing (must match the precision used for writing)
        int geohashPrecision = 5; // As defined in GeoUtils.getAutomaticShardKey

        // 1. Compute the rider's geohash
        GeoHash riderGeoHash = GeoHash.withCharacterPrecision(riderLat, riderLng, geohashPrecision);

        // 2. Determine which neighboring cells the search radius could touch
        Set<GeoHash> relevantGeoHashes = new HashSet<>();
        relevantGeoHashes.add(riderGeoHash); // Add the rider's own geohash
        relevantGeoHashes.addAll(List.of(riderGeoHash.getAdjacent())); // Add all 8 neighbors

        // Prepare for GEOSEARCH
        Circle queryArea = new Circle(new Point(riderLng, riderLat), new Distance(radiusInKm, Metrics.KILOMETERS));
        RedisGeoCommands.GeoSearchCommandArgs args = RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs()
                .includeDistance()
                .sortAscending(); // Sorting will be done after merging

        Set<GeoResult<RedisGeoCommands.GeoLocation<String>>> combinedResults = new HashSet<>();

        // 3. Run GEOSEARCH on each relevant shard
        for (GeoHash gh : relevantGeoHashes) {
            String shardedKey = "driver_locations:" + gh.toBase32();
            List<GeoResult<RedisGeoCommands.GeoLocation<String>>> shardResults = redisTemplate.opsForGeo()
                    .search(shardedKey, queryArea)
                    .getContent();
            combinedResults.addAll(shardResults);
        }

        // 4. Merge and dedupe results (handled by using a Set)
        // 5. Sort by distance
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> sortedResults = new ArrayList<>(combinedResults);
        sortedResults.sort(Comparator.comparing(geoResult -> geoResult.getDistance().getValue()));

        return sortedResults;
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