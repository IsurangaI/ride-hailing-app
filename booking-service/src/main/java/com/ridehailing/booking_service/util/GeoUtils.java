package com.ridehailing.booking_service.util;

public final class GeoUtils {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private GeoUtils() {
    }

    /**
     * Great-circle ("as the crow flies") distance between two points in kilometres.
     * This is not the driven route distance — it is a lower bound on it.
     */
    public static double haversineKm(double startLatitude, double startLongitude, double endLatitude, double endLongitude) {
        double deltaLatitude = Math.toRadians(endLatitude - startLatitude);
        double deltaLongitude = Math.toRadians(endLongitude - startLongitude);

        double a = Math.pow(Math.sin(deltaLatitude / 2), 2)
                + Math.cos(Math.toRadians(startLatitude)) * Math.cos(Math.toRadians(endLatitude))
                * Math.pow(Math.sin(deltaLongitude / 2), 2);

        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
