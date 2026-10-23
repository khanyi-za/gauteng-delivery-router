package com.khanyi.java.gautenglogistics.geo;

/** Straight-line distance between two points on the earth's surface. */
public final class Haversine {

    /** Mean radius of the earth in metres. */
    private static final double EARTH_RADIUS_M = 6_371_008.8;

    private Haversine() {
    }

    public static double distanceMetres(Coordinate a, Coordinate b) {
        double lat1 = Math.toRadians(a.lat());
        double lat2 = Math.toRadians(b.lat());
        double dLat = lat2 - lat1;
        double dLon = Math.toRadians(b.lon() - a.lon());

        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_RADIUS_M * Math.asin(Math.sqrt(h));
    }
}