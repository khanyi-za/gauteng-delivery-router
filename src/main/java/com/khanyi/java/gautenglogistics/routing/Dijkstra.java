package com.khanyi.java.gautenglogistics.routing;

import com.khanyi.java.gautenglogistics.graph.Edge;
import com.khanyi.java.gautenglogistics.graph.RoadGraph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.function.LongPredicate;

/**
 * Finds fastest routes with Dijkstra's algorithm.
 *
 * <p>It spreads out from the start like a ripple, always continuing from the intersection that
 * is quickest to reach so far. The first time it takes the destination off the queue, no faster
 * way there can exist, so it stops.
 */
public final class Dijkstra {

    private Dijkstra() {
    }

    /** The fastest route from one intersection to another. */
    public static Optional<Route> fastestRoute(RoadGraph graph, long from, long to) {
        return search(graph, from, node -> node == to, false);
    }

    /**
     * Searches backwards from {@code to}, driving every road in reverse, and stops at the first
     * intersection that {@code isStart} accepts. That's the one with the fastest drive <i>to</i>
     * {@code to}. With one-way streets, that isn't always the one with the fastest drive from it.
     *
     * @return the route from the intersection found to {@code to}, in driving order
     */
    public static Optional<Route> fastestRouteFromNearest(RoadGraph graph, long to, LongPredicate isStart) {
        return search(graph, to, isStart, true);
    }

    /**
     * The search itself. Going backwards, it follows the roads that arrive at each intersection
     * instead of the ones that leave it.
     */
    private static Optional<Route> search(RoadGraph graph, long origin, LongPredicate isGoal, boolean backwards) {
        Map<Long, Double> bestTime = new HashMap<>();   // fastest time found so far to each intersection
        Map<Long, Edge> arrivedBy = new HashMap<>();    // the road used to get there, to rebuild the route
        PriorityQueue<QueueEntry> queue = new PriorityQueue<>();
        bestTime.put(origin, 0.0);
        queue.add(new QueueEntry(origin, 0.0));
        int visited = 0;

        while (!queue.isEmpty()) {
            QueueEntry current = queue.poll();
            if (current.timeS() > bestTime.get(current.nodeId())) {
                continue; // an old entry: a faster way here was already found
            }
            visited++;
            if (isGoal.test(current.nodeId())) {
                return Optional.of(buildRoute(arrivedBy, origin, current.nodeId(), current.timeS(), visited, backwards));
            }
            List<Edge> roads = backwards ? graph.incoming(current.nodeId()) : graph.outgoing(current.nodeId());
            for (Edge edge : roads) {
                long next = backwards ? edge.from() : edge.to();
                double time = current.timeS() + edge.travelTimeS();
                if (time < bestTime.getOrDefault(next, Double.POSITIVE_INFINITY)) {
                    bestTime.put(next, time);
                    arrivedBy.put(next, edge);
                    queue.add(new QueueEntry(next, time));
                }
            }
        }
        return Optional.empty(); // nothing the search was looking for can be reached
    }

    /**
     * Walks from the intersection found back to the origin along the roads used. Going forwards,
     * that gives the roads last to first, so the list is turned around. Going backwards, it's
     * already in driving order.
     */
    private static Route buildRoute(Map<Long, Edge> arrivedBy, long origin, long found, double timeS,
                                    int visited, boolean backwards) {
        List<Edge> edges = new ArrayList<>();
        double lengthM = 0;
        long node = found;
        while (node != origin) {
            Edge edge = arrivedBy.get(node);
            edges.add(edge);
            lengthM += edge.lengthM();
            node = backwards ? edge.to() : edge.from();
        }
        if (!backwards) {
            Collections.reverse(edges);
        }
        return new Route(edges, timeS, lengthM, visited);
    }

    /**
     * An intersection waiting in the queue, with the time to reach it. The queue always gives back
     * the one with the smallest time first.
     */
    private record QueueEntry(long nodeId, double timeS) implements Comparable<QueueEntry> {

        @Override
        public int compareTo(QueueEntry other) {
            return Double.compare(timeS, other.timeS);
        }
    }
}