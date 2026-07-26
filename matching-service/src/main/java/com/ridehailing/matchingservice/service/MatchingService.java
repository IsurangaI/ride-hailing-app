package com.ridehailing.matchingservice.service;

import ch.hsr.geohash.GeoHash;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.ridehailing.matchingservice.exception.NoDriversAvailableException;
import com.ridehailing.matchingservice.model.event.RideOfferedEvent;
import com.ridehailing.matchingservice.model.event.RideRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.data.redis.domain.geo.GeoShape;
import org.springframework.data.redis.domain.geo.Metrics;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchingService {
    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public List<GeoResult<RedisGeoCommands.GeoLocation<String>>> findNearbyDrivers(double riderLng, double riderLat, double radiusInKm) {
         // Must match the precision used when writing, see GeoUtils.getAutomaticShardKey
        int geohashPrecision = 5;

        GeoHash riderGeoHash = GeoHash.withCharacterPrecision(riderLat, riderLng, geohashPrecision);

        // Cover the rider's own cell plus all 8 neighbors so boundary drivers aren't missed
        Set<GeoHash> relevantGeoHashes = new HashSet<>();
        relevantGeoHashes.add(riderGeoHash);
        relevantGeoHashes.addAll(List.of(riderGeoHash.getAdjacent()));

        Circle queryArea = new Circle(new Point(riderLng, riderLat), new Distance(radiusInKm, Metrics.KILOMETERS));
        RedisGeoCommands.GeoSearchCommandArgs args = RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs()
                .includeDistance()
                .sortAscending();

        // A Set dedupes drivers that surface from more than one shard
        Set<GeoResult<RedisGeoCommands.GeoLocation<String>>> combinedResults = new HashSet<>();

        for (GeoHash gh : relevantGeoHashes) {
            String shardedKey = "driver_locations:" + gh.toBase32();
            List<GeoResult<RedisGeoCommands.GeoLocation<String>>> shardResults = redisTemplate.opsForGeo()
                    .search(shardedKey, GeoReference.fromCoordinate(queryArea.getCenter()),
                            GeoShape.byRadius(queryArea.getRadius()), args)
                    .getContent();
            combinedResults.addAll(shardResults);
        }

        // Per-shard sorting doesn't survive the merge, so sort the combined set here
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> sortedResults = new ArrayList<>(combinedResults);
        sortedResults.sort(Comparator.comparing(geoResult -> geoResult.getDistance().getValue()));

        return sortedResults;
    }

    public void findAndAssignDriver(RideRequestedEvent event) throws JsonProcessingException {
        log.info("Initiating driver search for Booking ID: {}", event.getBookingId());

        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> nearbyDrivers =
                findNearbyDrivers(event.getPickupLongitude(), event.getPickupLatitude(), 3.0);

        if (nearbyDrivers.isEmpty()) {
            // TODO: publish a RideUnmatchedEvent so booking-service can mark the ride FAILED
            log.warn("No drivers found within 3km for Booking ID: {}. Search aborted.", event.getBookingId());
            return;
        }

        // Already sorted by distance, so the first driver who hasn't rejected this ride is the closest
        String closestDriver = nearbyDrivers.stream()
                .filter(driver -> !event.getRejectedDrivers().contains(driver.getContent().getName()))
                .map(driver -> driver.getContent().getName())
                .findFirst()
                .orElseThrow(NoDriversAvailableException::new);

        kafkaTemplate.send("ride-offers", new RideOfferedEvent(event.getBookingId(), closestDriver));
        log.info("Offered Booking ID {} to driver {}", event.getBookingId(), closestDriver);
    }
}
