package com.ridehailing.matchingservice.service;

import ch.hsr.geohash.GeoHash;
import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.domain.geo.Metrics;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class MatchingService {
    private final RedisTemplate<String, String> redisTemplate;

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
}
