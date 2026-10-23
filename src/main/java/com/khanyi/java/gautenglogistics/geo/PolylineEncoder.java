package com.khanyi.java.gautenglogistics.geo;

import java.util.List;

/**
 * Encodes a list of points in the Encoded Polyline format (popularised by Google Maps),
 * which map libraries such as Leaflet can decode. Each point is stored as the difference
 * from the previous one, at 5 decimal places (about 1 metre), in a few printable characters.
 */
public final class PolylineEncoder {

    private static final double PRECISION = 1e5;

    private PolylineEncoder() {
    }

    public static String encode(List<Coordinate> points) {
        StringBuilder out = new StringBuilder(points.size() * 6);
        long previousLat = 0;
        long previousLon = 0;
        for (Coordinate point : points) {
            long lat = Math.round(point.lat() * PRECISION);
            long lon = Math.round(point.lon() * PRECISION);
            encodeValue(lat - previousLat, out);
            encodeValue(lon - previousLon, out);
            previousLat = lat;
            previousLon = lon;
        }
        return out.toString();
    }

    private static void encodeValue(long value, StringBuilder out) {
        long v = value < 0 ? ~(value << 1) : value << 1; // move the sign into the lowest bit
        while (v >= 0x20) {
            out.append((char) ((0x20 | (v & 0x1f)) + 63)); // 5 bits per character, 0x20 = "more follows"
            v >>= 5;
        }
        out.append((char) (v + 63));
    }
}
