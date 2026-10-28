package com.khanyi.java.gautenglogistics.routing;

import com.khanyi.java.gautenglogistics.graph.Edge;

import java.util.List;

/**
 * The fastest way from one intersection to another.
 *
 * @param edges        the road pieces to drive, in order
 * @param travelTimeS  total driving time in seconds
 * @param lengthM      total distance in metres
 * @param nodesVisited how many intersections the search had to look at (shows how much work it did)
 */
public record Route(List<Edge> edges, double travelTimeS, double lengthM, int nodesVisited) {
}

