package com.khanyi.java.gautenglogistics.geo;

import java.util.ArrayList;
import java.util.List;

/** Turns an Encoded Polyline (see {@link PolylineEncoder}) back into a list of points. */
public final class PolylineDecoder {

    private static final double PRECISION = 1e5;

    private PolylineDecoder() {
    }

    public static List<Coordinate> decode(String encoded) {
        List<Coordinate> points = new ArrayList<>();
        // position[0] is where we are in the text; an array so decodeValue can move it forward
        int[] position = {0};
        long lat = 0;
        long lon = 0;
        while (position[0] < encoded.length()) {
            lat += decodeValue(encoded, position); // each value is the change from the previous point
            lon += decodeValue(encoded, position);
            points.add(new Coordinate(lat / PRECISION, lon / PRECISION));
        }
        return points;
    }

    /** Reads one number, 5 bits per character, the reverse of PolylineEncoder.encodeValue. */
    private static long decodeValue(String encoded, int[] position) {
        long v = 0;
        int shift = 0;
        int chunk;
        do {
            if (position[0] >= encoded.length()) {
                throw new IllegalArgumentException("Polyline ends in the middle of a number: " + encoded);
            }
            chunk = encoded.charAt(position[0]++) - 63;
            v |= (long) (chunk & 0x1f) << shift;
            shift += 5;
        } while (chunk >= 0x20); // 0x20 = "more follows"
        return (v & 1) == 0 ? v >> 1 : ~(v >> 1); // the lowest bit is the sign
    }
}
