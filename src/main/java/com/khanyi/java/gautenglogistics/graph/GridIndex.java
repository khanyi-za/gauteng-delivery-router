package com.khanyi.java.gautenglogistics.graph;

import com.khanyi.java.gautenglogistics.geo.Coordinate;
import com.khanyi.java.gautenglogistics.geo.Haversine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Finds the intersection nearest to a GPS point quickly.
 *
 * <p>The map is cut into squares about 500 m wide, like a sheet of graph paper, and each
 * intersection is filed under the square it's in. To find the nearest one, we look in the
 * point's own square first, then the ring of squares around it, then the next ring, and so on,
 * instead of measuring the distance to all 450,000 intersections.
 *
 * <p>Only intersections on roads you may start or end a delivery on are filed, so a customer
 * is never matched to a freeway or an on-ramp.
 */
public final class GridIndex {

    /** Size of each square in degrees: about 550 m north to south and 500 m east to west in Gauteng. */
    private static final double CELL_DEGREES = 0.005;

    private final Map<Long, List<RoadNode>> cells = new HashMap<>();
    private final double maxSnapDistanceM;

    public GridIndex(RoadGraph graph, double maxSnapDistanceM) {
        this.maxSnapDistanceM = maxSnapDistanceM;
        for (RoadNode node : graph.nodes()) {
            if (onSnappableRoad(graph, node.id())) {
                long key = cellKey(cellOf(node.location().lat()), cellOf(node.location().lon()));
                cells.computeIfAbsent(key, k -> new ArrayList<>()).add(node);
            }
        }
    }

    /**
     * Returns the nearest intersection, or nothing if there is none within the maximum
     * snap distance (for example, a point outside Gauteng).
     */
    public Optional<Snap> nearest(Coordinate point) {
        int row = cellOf(point.lat());
        int col = cellOf(point.lon());
        double cellSizeM = smallestCellSideM(point);
        int maxRing = (int) Math.ceil(maxSnapDistanceM / cellSizeM) + 1;

        RoadNode best = null;
        double bestM = Double.POSITIVE_INFINITY;
        for (int ring = 0; ring <= maxRing; ring++) {
            for (int dRow = -ring; dRow <= ring; dRow++) {
                for (int dCol = -ring; dCol <= ring; dCol++) {
                    if (Math.max(Math.abs(dRow), Math.abs(dCol)) != ring) {
                        continue; // inside the ring: already checked
                    }
                    for (RoadNode node : cells.getOrDefault(cellKey(row + dRow, col + dCol), List.of())) {
                        double m = Haversine.distanceMetres(point, node.location());
                        if (m < bestM) {
                            best = node;
                            bestM = m;
                        }
                    }
                }
            }
            // Every square further out is at least ring * cellSizeM away, so nothing there can be closer
            if (best != null && bestM <= ring * cellSizeM) {
                break;
            }
        }

        if (best == null || bestM > maxSnapDistanceM) {
            return Optional.empty();
        }
        return Optional.of(new Snap(best, bestM));
    }

    private static boolean onSnappableRoad(RoadGraph graph, long nodeId) {
        return graph.outgoing(nodeId).stream().anyMatch(edge -> edge.roadClass().snappable())
                || graph.incoming(nodeId).stream().anyMatch(edge -> edge.roadClass().snappable());
    }

    private static int cellOf(double degrees) {
        return (int) Math.floor(degrees / CELL_DEGREES);
    }

    /** Packs a row and column into one number, to use as the map key. */
    private static long cellKey(int row, int col) {
        return ((long) row << 32) | (col & 0xffffffffL);
    }

    /** Squares are narrower east to west than north to south (lines of longitude get closer together). */
    private static double smallestCellSideM(Coordinate point) {
        Coordinate north = new Coordinate(point.lat() + CELL_DEGREES, point.lon());
        Coordinate east = new Coordinate(point.lat(), point.lon() + CELL_DEGREES);
        return Math.min(Haversine.distanceMetres(point, north), Haversine.distanceMetres(point, east));
    }
}