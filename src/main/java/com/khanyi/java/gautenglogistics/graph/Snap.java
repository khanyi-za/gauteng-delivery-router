package com.khanyi.java.gautenglogistics.graph;


/**
 * The intersection closest to a GPS point, where a route can start or end.
 *
 * @param node      the intersection
 * @param distanceM how far the GPS point is from it, in a straight line, in metres
 */
public record Snap(RoadNode node, double distanceM) {
}
