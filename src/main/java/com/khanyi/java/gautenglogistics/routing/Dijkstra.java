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

/**
 * Finds the fastest route between two intersections with Dijkstra's algorithm.
 *
 * <p>It spreads out from the start like a ripple, always continuing from the intersection that
 * is quickest to reach so far. The first time it takes the destination off the queue, no faster
 * way there can exist, so it stops.
 */
public final class Dijkstra {

    private Dijkstra() {
    }

    public static Optional<Route> fastestRoute(RoadGraph graph, long from, long to) {
        Map<Long, Double> bestTime = new HashMap<>();   // fastest time found so far to each intersection
        Map<Long, Edge> arrivedBy = new HashMap<>();    // the road used to get there, to rebuild the route
        PriorityQueue<QueueEntry> queue = new PriorityQueue<>();
        bestTime.put(from, 0.0);
        queue.add(new QueueEntry(from, 0.0));
        int visited = 0;

        while (!queue.isEmpty()) {
            QueueEntry current = queue.poll();
            if (current.timeS() > bestTime.get(current.nodeId())) {
                continue; // an old entry: a faster way here was already found
            }
            visited++;
            if (current.nodeId() == to) {
                return Optional.of(buildRoute(arrivedBy, from, to, current.timeS(), visited));
            }
            for (Edge edge : graph.outgoing(current.nodeId())) {
                double time = current.timeS() + edge.travelTimeS();
                if (time < bestTime.getOrDefault(edge.to(), Double.POSITIVE_INFINITY)) {
                    bestTime.put(edge.to(), time);
                    arrivedBy.put(edge.to(), edge);
                    queue.add(new QueueEntry(edge.to(), time));
                }
            }
        }
        return Optional.empty(); // the destination can't be reached
    }

    /** Walks back from the destination to the start along the roads used, then turns the list around. */
    private static Route buildRoute(Map<Long, Edge> arrivedBy, long from, long to, double timeS, int visited) {
        List<Edge> edges = new ArrayList<>();
        double lengthM = 0;
        long node = to;
        while (node != from) {
            Edge edge = arrivedBy.get(node);
            edges.add(edge);
            lengthM += edge.lengthM();
            node = edge.from();
        }
        Collections.reverse(edges);
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
