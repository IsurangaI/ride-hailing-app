package com.ridehailing.location_service.util;

import ch.hsr.geohash.GeoHash;

public class GeoUtils {

    // Strategy A: Geohashing (Algorithmic Grid Sharding)
    // Converts lat/lng into an exact grid location. We take a substring prefix to determine shard size.
    public static String convertToGeohashShard(double longitude, double latitude) {
        // Character count determines precision (e.g., 5 chars is ~4.9km x 4.9km grid)
        int precision = 5;
        return GeoHash.withCharacterPrecision(latitude, longitude, precision).toBase32();
    }

    // Strategy B: Bounding Boxes (Logical Regional Boundaries)
    // Best if your business operations are strictly isolated by municipal boundaries
    public static String resolveRegionByCoordinates(double longitude, double latitude) {
        // Berlin rough bounding box
        if (latitude >= 52.33 && latitude <= 52.67 && longitude >= 13.08 && longitude <= 13.76) {
            return "berlin";
        }
        // Paris rough bounding box
        if (latitude >= 48.81 && latitude <= 48.90 && longitude >= 2.22 && longitude <= 2.47) {
            return "paris";
        }

        // Fallback default shard for unsupported areas
        return "global_fallback";
    }

    public static String getAutomaticShardKey(double longitude, double latitude) {
        // A 5-character geohash represents a grid cell of roughly 4.9km x 4.9km.
        int precision = 5;

        // FIX: Pass latitude FIRST, then longitude
        String geohash = GeoHash.withCharacterPrecision(latitude, longitude, precision).toBase32();

        return "driver_locations:" + geohash;
    }
}
