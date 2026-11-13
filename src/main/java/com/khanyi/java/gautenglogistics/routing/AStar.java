package com.khanyi.java.gautenglogistics.routing;

import com.khanyi.java.gautenglogistics.geo.Coordinate;
import com.khanyi.java.gautenglogistics.geo.Haversine;
import com.khanyi.java.gautenglogistics.graph.RoadClass;
import com.khanyi.java.gautenglogistics.graph.RoadGraph;

import java.util.Optional;

/**
 * Finds fastest routes with A* ("A star"). It's Dijkstra's search, but each intersection waiting
 * in the queue also gets a guess of the time left to the destination. Intersections that head
 * away from the destination get big guesses, so the search mostly leaves them alone.
 *
 * <p>The guess is the straight-line distance driven at the top speed of any road. No real drive
 * can be faster than that, so the guess is never too big, and the route found is still the fastest.
 */
public final class AStar {

    /** The top speed of any road, in metres per second. */
    private static final double MAX_SPEED_MS = RoadClass.MAX_SPEED_KMH / 3.6;

    private AStar() {
    }

    /** The fastest route from one intersection to another. */
    public static Optional<Route> fastestRoute(RoadGraph graph, long from, long to) {
        Coordinate destination = graph.node(to).location();
        return Dijkstra.search(graph, from, node -> node == to, false,
                node -> Haversine.distanceMetres(graph.node(node).location(), destination) / MAX_SPEED_MS);
    }
}
