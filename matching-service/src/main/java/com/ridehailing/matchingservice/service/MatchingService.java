package com.ridehailing.matchingservice.service;

import ch.hsr.geohash.GeoHash;
import com.ridehailing.matchingservice.model.event.DriverMatchedEvent;
import com.ridehailing.matchingservice.model.event.RideRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.domain.geo.Metrics;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchingService {
    private static final String MATCHED = "MATCHED";
    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

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

    public void findAndAssignDriver(RideRequestedEvent event) {
        log.info("Initiating driver search for Booking ID: {}", event.getBookingId());

        // 1. Execute your geospatial search (e.g., 3.0 kilometer radius)
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> nearbyDrivers =
                findNearbyDrivers(event.getPickupLongitude(), event.getPickupLatitude(), 3.0);

        // 2. Handle the scenario where no drivers are nearby
        if (nearbyDrivers.isEmpty()) {
            log.warn("No drivers found within 3km for Booking ID: {}. Search aborted.", event.getBookingId());
            // In a production system, you might publish a "RideUnmatchedEvent" here
            // so the Booking Service can update the DB status to "FAILED" and notify the user.
            return;
        }

        // 3. Select the optimal driver
        // Because your findNearbyDrivers method already sorted them by distance,
        // index 0 is guaranteed to be the closest driver.
        GeoResult<RedisGeoCommands.GeoLocation<String>> optimalDriver = nearbyDrivers.get(0);
        String selectedDriverId = optimalDriver.getContent().getName();
        double distanceToRider = optimalDriver.getDistance().getValue();

        log.info("Matched Driver {} at {} km away for Booking ID: {}",
                selectedDriverId, distanceToRider, event.getBookingId());

        // 4. Publish the DriverMatchedEvent
        DriverMatchedEvent matchEvent = new DriverMatchedEvent(
                event.getBookingId(),
                selectedDriverId,
                MATCHED
        );

        // Drop the event into the new topic for the Booking Service to consume
        kafkaTemplate.send("driver-matches", matchEvent);
        log.info("Successfully published DriverMatchedEvent to Kafka topic 'driver-matches'.");
    }
}
