package com.khanyi.java.gautenglogistics.ingest;

import com.khanyi.java.gautenglogistics.geo.Coordinate;
import com.khanyi.java.gautenglogistics.geo.Haversine;
import com.khanyi.java.gautenglogistics.geo.PolylineEncoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Splits drivable roads into directed edges between intersections.
 *
 * <p>A node becomes a graph vertex (an intersection) if it is the end of a road or is used
 * more than once: shared by two roads, or visited twice by the same road. Every other node
 * is a shape point that only bends the road, so it goes into the edge's length and geometry
 * but not into the graph.
 */
public final class EdgeBuilder {

    private EdgeBuilder() {
    }

    /**
     * Counts how often each node is used by the roads. Road ends count twice, so that
     * "count >= 2" is exactly the test for being a vertex. The key set is every node
     * the roads need coordinates for.
     */
    public static Map<Long, Integer> countNodeUses(List<DrivableWay> ways) {
        Map<Long, Integer> uses = new HashMap<>();
        for (DrivableWay way : ways) {
            long[] nodeIds = way.nodeIds();
            for (int i = 0; i < nodeIds.length; i++) {
                boolean end = i == 0 || i == nodeIds.length - 1;
                uses.merge(nodeIds[i], end ? 2 : 1, Integer::sum);
            }
        }
        return uses;
    }

    public static boolean isVertex(long nodeId, Map<Long, Integer> nodeUses) {
        return nodeUses.getOrDefault(nodeId, 0) >= 2;
    }

    /**
     * Cuts every road at its vertices. Two-way roads give an edge in each direction.
     * A stretch of road with a node whose coordinates are missing (a road crossing the edge
     * of the map extract) is dropped, as are edges that start and end at the same node.
     */
    public static List<RawEdge> buildEdges(List<DrivableWay> ways, Map<Long, Integer> nodeUses,
                                           Map<Long, Coordinate> coordinates) {
        List<RawEdge> edges = new ArrayList<>();
        for (DrivableWay way : ways) {
            splitWay(way, nodeUses, coordinates, edges);
        }
        return edges;
    }

    private static void splitWay(DrivableWay way, Map<Long, Integer> nodeUses,
                                 Map<Long, Coordinate> coordinates, List<RawEdge> edges) {
        long[] nodeIds = way.nodeIds();
        long startNode = nodeIds[0];
        List<Coordinate> points = new ArrayList<>();
        double lengthM = 0;
        boolean complete = coordinates.containsKey(startNode);
        if (complete) {
            points.add(coordinates.get(startNode));
        }

        for (int i = 1; i < nodeIds.length; i++) {
            Coordinate point = coordinates.get(nodeIds[i]);
            if (point == null) {
                complete = false;
            } else if (complete) {
                lengthM += Haversine.distanceMetres(points.getLast(), point);
                points.add(point);
            }

            boolean last = i == nodeIds.length - 1;
            if (isVertex(nodeIds[i], nodeUses) || last) {
                if (complete && startNode != nodeIds[i]) {
                    addEdges(way, startNode, nodeIds[i], lengthM, points, edges);
                }
                // The next edge starts here
                startNode = nodeIds[i];
                points = new ArrayList<>();
                lengthM = 0;
                complete = point != null;
                if (complete) {
                    points.add(point);
                }
            }
        }
    }

    private static void addEdges(DrivableWay way, long from, long to, double lengthM,
                                 List<Coordinate> points, List<RawEdge> edges) {
        double travelTimeS = lengthM / (way.speedKmh() / 3.6);
        edges.add(new RawEdge(from, to, way.osmId(), way.roadClass(), lengthM, travelTimeS,
                PolylineEncoder.encode(points)));
        if (!way.oneWay()) {
            edges.add(new RawEdge(to, from, way.osmId(), way.roadClass(), lengthM, travelTimeS,
                    PolylineEncoder.encode(points.reversed())));
        }
    }
}
