package com.khanyi.java.gautenglogistics.routing;

import com.khanyi.java.gautenglogistics.geo.Coordinate;
import com.khanyi.java.gautenglogistics.graph.Snap;

import java.util.List;

/**
 * A route between two GPS points.
 *
 * @param start the intersection the start point was matched to
 * @param end   the intersection the end point was matched to
 * @param route the fastest route between them
 * @param path  the shape of the route along the real roads, from start to end
 */
public record RoutePlan(Snap start, Snap end, Route route, List<Coordinate> path) {
}
