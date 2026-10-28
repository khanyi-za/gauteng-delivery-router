package com.khanyi.java.gautenglogistics.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The whole road map held in memory, so route searches never wait for the database.
 *
 * <p>For every intersection it keeps the roads going out of it (to find routes from a store to a
 * customer) and the roads coming into it (to search backwards from a customer to the nearest store).
 * Once built it never changes, so many requests can read it at the same time safely.
 */
public final class RoadGraph {

    private final Map<Long, RoadNode> nodes;
    private final Map<Long, List<Edge>> outgoing;
    private final Map<Long, List<Edge>> incoming;
    private final int edgeCount;

    public RoadGraph(Collection<RoadNode> nodes, Collection<Edge> edges) {
        this.nodes = new HashMap<>(nodes.size() * 4 / 3 + 1);
        for (RoadNode node : nodes) {
            this.nodes.put(node.id(), node);
        }
        this.outgoing = new HashMap<>(nodes.size() * 4 / 3 + 1);
        this.incoming = new HashMap<>(nodes.size() * 4 / 3 + 1);
        for (Edge edge : edges) {
            if (!this.nodes.containsKey(edge.from()) || !this.nodes.containsKey(edge.to())) {
                throw new IllegalArgumentException("Edge " + edge.id() + " joins an intersection that isn't in the graph");
            }
            outgoing.computeIfAbsent(edge.from(), id -> new ArrayList<>(4)).add(edge);
            incoming.computeIfAbsent(edge.to(), id -> new ArrayList<>(4)).add(edge);
        }
        this.edgeCount = edges.size();
    }

    public RoadNode node(long id) {
        return nodes.get(id);
    }

    public Collection<RoadNode> nodes() {
        return nodes.values();
    }

    /** The roads you can drive along from this intersection. */
    public List<Edge> outgoing(long nodeId) {
        return outgoing.getOrDefault(nodeId, List.of());
    }

    /** The roads that arrive at this intersection. */
    public List<Edge> incoming(long nodeId) {
        return incoming.getOrDefault(nodeId, List.of());
    }

    public int nodeCount() {
        return nodes.size();
    }

    public int edgeCount() {
        return edgeCount;
    }
}
